package com.goevent.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.goevent.AppContainer
import com.goevent.GoEventApp
import com.goevent.core.model.Achievement
import com.goevent.core.model.Category
import com.goevent.core.model.Event
import com.goevent.core.model.EventQuery
import com.goevent.core.model.GoPointsHistoryItem
import com.goevent.core.model.NotificationItem
import com.goevent.core.model.Reward
import com.goevent.core.model.RewardRedemption
import com.goevent.core.model.TicketDetail
import com.goevent.core.model.TicketType
import com.goevent.core.model.UserAchievement
import com.goevent.core.model.UserProfile
import com.goevent.data.BookingResult
import com.goevent.data.PaymentVerificationResult
import com.goevent.data.RewardRedemptionResult
import com.goevent.data.TicketValidationResult
import com.goevent.data.toUserMessage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Creates a ViewModel that can read the app's repositories. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as GoEventApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

// =========================================================== session (login, profile, settings)

data class SessionState(
    val restoring: Boolean = true,
    val restoreFailed: Boolean = false,
    val restoreError: String? = null,
    val prefsLoaded: Boolean = false,
    val profile: UserProfile? = null,
    val onboardingDone: Boolean = false,
    val theme: String = "System",
    val language: String = "English",
    val notifyEvents: Boolean = true,
    val notifyRewards: Boolean = true,
    val notifyBookings: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class SessionViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                c.dataStore.themeFlow, c.dataStore.languageFlow, c.dataStore.onboardingCompletedFlow,
                c.dataStore.notifyEventsFlow, c.dataStore.notifyRewardsFlow, c.dataStore.notifyBookingsFlow
            ) { values -> values }.collect { v ->
                _state.update {
                    it.copy(
                        theme = v[0] as String, language = v[1] as String, onboardingDone = v[2] as Boolean,
                        notifyEvents = v[3] as Boolean, notifyRewards = v[4] as Boolean, notifyBookings = v[5] as Boolean,
                        prefsLoaded = true
                    )
                }
            }
        }
        viewModelScope.launch {
            c.authRepository.authState.collect { uid -> if (uid == null) _state.update { it.copy(profile = null) } }
        }
        restore()
    }

    fun restore() {
        _state.update { it.copy(restoring = true, restoreFailed = false, restoreError = null) }
        viewModelScope.launch {
            val result = c.authRepository.restoreSession()
            _state.update {
                it.copy(
                    restoring = false, profile = result.getOrNull(),
                    restoreFailed = result.isFailure, restoreError = result.exceptionOrNull()?.toUserMessage()
                )
            }
        }
    }

    private fun authCall(onSuccess: () -> Unit, block: suspend () -> Result<UserProfile>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, info = null) }
        viewModelScope.launch {
            block()
                .onSuccess { p -> _state.update { it.copy(busy = false, profile = p) }; onSuccess() }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUserMessage()) } }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) =
        authCall(onSuccess) { c.authRepository.signIn(email, password) }

    fun signInWithGoogle(onSuccess: () -> Unit) =
        authCall(onSuccess) { c.authRepository.signInWithGoogle() }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) =
        authCall(onSuccess) { c.authRepository.register(email, password, name) }

    fun sendPasswordReset(email: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, info = null) }
        viewModelScope.launch {
            c.authRepository.sendPasswordReset(email)
                .onSuccess { _state.update { s -> s.copy(busy = false, info = "If an account exists for $email, a reset link is on its way. Check your inbox and spam folder.") } }
                .onFailure { e -> _state.update { s -> s.copy(busy = false, error = e.toUserMessage()) } }
        }
    }

    fun changePassword(current: String, new: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, info = null) }
        viewModelScope.launch {
            c.authRepository.changePassword(current, new)
                .onSuccess { _state.update { s -> s.copy(busy = false, info = "Your password was changed.") } }
                .onFailure { e -> _state.update { s -> s.copy(busy = false, error = e.toUserMessage()) } }
        }
    }

    fun signOut() {
        c.authRepository.signOut()
        _state.update { it.copy(profile = null, error = null, info = null) }
    }

    fun refreshProfile() {
        val uid = c.authRepository.currentUserId ?: return
        viewModelScope.launch {
            c.profileRepository.fetchProfile(uid).onSuccess { p -> if (p != null) _state.update { it.copy(profile = p) } }
        }
    }

    fun updateProfile(name: String, avatarJpeg: ByteArray?, onDone: () -> Unit) {
        val uid = _state.value.profile?.id ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            var avatarUrl: String? = null
            if (avatarJpeg != null) {
                val upload = c.profileRepository.uploadAvatar(uid, avatarJpeg)
                if (upload.isFailure) {
                    _state.update { it.copy(busy = false, error = upload.exceptionOrNull()?.toUserMessage()) }
                    return@launch
                }
                avatarUrl = upload.getOrNull()
            }
            c.profileRepository.updateProfile(uid, name, avatarUrl)
                .onSuccess { _state.update { it.copy(busy = false) }; refreshProfile(); onDone() }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUserMessage()) } }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }
    fun setTheme(value: String) { viewModelScope.launch { c.dataStore.setTheme(value) } }
    fun setLanguage(value: String) { viewModelScope.launch { c.dataStore.setLanguage(value) } }
    fun completeOnboarding() { viewModelScope.launch { c.dataStore.setOnboardingCompleted(true) } }
    fun setNotifyEvents(v: Boolean) { viewModelScope.launch { c.dataStore.setNotifyEvents(v) } }
    fun setNotifyRewards(v: Boolean) { viewModelScope.launch { c.dataStore.setNotifyRewards(v) } }
    fun setNotifyBookings(v: Boolean) { viewModelScope.launch { c.dataStore.setNotifyBookings(v) } }
}

// =========================================================== favourites (shared by many screens)

class FavoritesViewModel(private val c: AppContainer) : ViewModel() {
    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun reload() {
        val uid = c.authRepository.currentUserId
        if (uid == null) { _ids.value = emptySet(); return }
        viewModelScope.launch { c.eventRepository.fetchFavoriteEventIds(uid).onSuccess { _ids.value = it.toSet() } }
    }

    fun toggle(eventId: String) {
        val uid = c.authRepository.currentUserId ?: return
        val nowFav = eventId !in _ids.value
        _ids.update { if (nowFav) it + eventId else it - eventId }
        viewModelScope.launch {
            c.eventRepository.setFavorite(uid, eventId, nowFav)
                .onSuccess { c.goPointsRepository.refreshAchievements() }
                .onFailure { e ->
                    _ids.update { if (nowFav) it - eventId else it + eventId }
                    _message.value = e.toUserMessage()
                }
        }
    }

    fun clearMessage() { _message.value = null }
}

// =========================================================== home

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val featured: List<Event> = emptyList(),
    val upcoming: List<Event> = emptyList(),
    val unread: Int = 0
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val cats = async { c.eventRepository.fetchCategories() }
            val featured = async { c.eventRepository.fetchEvents(EventQuery(featuredOnly = true, limit = 8)) }
            val upcoming = async { c.eventRepository.fetchEvents(EventQuery(limit = 12)) }
            val uid = c.authRepository.currentUserId
            val unread = async { uid?.let { c.notificationRepository.fetchNotifications(it).getOrNull()?.count { n -> !n.isRead } } ?: 0 }

            val catsR = cats.await(); val featR = featured.await(); val upR = upcoming.await()
            val up = upR.getOrDefault(emptyList())
            val feat = featR.getOrDefault(emptyList()).ifEmpty { up.take(6) }
            val error = if (catsR.isFailure && upR.isFailure) (upR.exceptionOrNull() ?: catsR.exceptionOrNull())?.toUserMessage() else null
            _state.update {
                it.copy(loading = false, error = error, categories = catsR.getOrDefault(emptyList()), featured = feat, upcoming = up, unread = unread.await())
            }
        }
    }
}

// =========================================================== explore / search

data class ExploreState(
    val query: EventQuery = EventQuery(),
    val loading: Boolean = true,
    val error: String? = null,
    val events: List<Event> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cities: List<String> = emptyList()
)

@OptIn(FlowPreview::class)
class ExploreViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()
    private val queryFlow = MutableStateFlow(EventQuery())

    init {
        viewModelScope.launch {
            c.eventRepository.fetchCategories().onSuccess { list -> _state.update { it.copy(categories = list) } }
            c.eventRepository.fetchCities().onSuccess { list -> _state.update { it.copy(cities = list) } }
        }
        viewModelScope.launch {
            queryFlow.debounce(300).collectLatest { q ->
                val result = c.eventRepository.fetchEvents(q)
                _state.update {
                    it.copy(loading = false, events = result.getOrDefault(emptyList()), error = result.exceptionOrNull()?.toUserMessage())
                }
            }
        }
    }

    fun update(change: (EventQuery) -> EventQuery) {
        val q = change(_state.value.query)
        _state.update { it.copy(query = q, loading = true) }
        queryFlow.value = q
    }

    fun retry() = update { it.copy() }
}

// =========================================================== event details + booking selection

data class EventDetailsState(
    val loading: Boolean = true,
    val error: String? = null,
    val event: Event? = null,
    val types: List<TicketType> = emptyList(),
    val categoryName: String? = null,
    val categoryIcon: String? = null
)

class EventDetailsViewModel(private val c: AppContainer, private val eventId: String) : ViewModel() {
    private val _state = MutableStateFlow(EventDetailsState())
    val state: StateFlow<EventDetailsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val ev = async { c.eventRepository.fetchEventById(eventId) }
            val types = async { c.eventRepository.fetchTicketTypes(eventId) }
            val cats = async { c.eventRepository.fetchCategories() }
            val evR = ev.await()
            val event = evR.getOrNull()
            val cat = cats.await().getOrNull()?.firstOrNull { it.id == event?.categoryId }
            _state.update {
                it.copy(
                    loading = false, event = event, types = types.await().getOrDefault(emptyList()),
                    error = if (event == null) (evR.exceptionOrNull()?.toUserMessage() ?: "Event not found") else null,
                    categoryName = cat?.name, categoryIcon = cat?.iconName
                )
            }
        }
    }
}

// =========================================================== checkout + payment

data class CheckoutState(
    val loading: Boolean = true,
    val error: String? = null,
    val event: Event? = null,
    val type: TicketType? = null,
    val points: Int = 0,
    val busy: Boolean = false,
    val submitError: String? = null
)

class CheckoutViewModel(private val c: AppContainer, private val eventId: String, private val typeId: String) : ViewModel() {
    private val _state = MutableStateFlow(CheckoutState())
    val state: StateFlow<CheckoutState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val ev = async { c.eventRepository.fetchEventById(eventId) }
            val types = async { c.eventRepository.fetchTicketTypes(eventId) }
            val event = ev.await().getOrNull()
            val type = types.await().getOrNull()?.firstOrNull { it.id == typeId }
            _state.update { it.copy(loading = false, event = event, type = type, error = if (event == null || type == null) "This ticket is no longer available." else null) }
        }
    }

    fun setPoints(value: Int) = _state.update { it.copy(points = value.coerceAtLeast(0)) }

    /** The server re-checks price, stock and points; this only sends the user's choices. */
    fun confirm(quantity: Int, redeemPoints: Int, onFree: (BookingResult) -> Unit, onPay: (BookingResult) -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, submitError = null) }
        viewModelScope.launch {
            c.bookingRepository.createBooking(eventId, typeId, quantity, redeemPoints)
                .onSuccess { r ->
                    _state.update { it.copy(busy = false) }
                    if (r.isFree) onFree(r) else onPay(r)
                }
                .onFailure { e -> _state.update { it.copy(busy = false, submitError = e.toUserMessage()) } }
        }
    }
}

data class PaymentState(val busy: Boolean = false, val error: String? = null, val finished: Boolean = false)

class PaymentViewModel(private val c: AppContainer, private val bookingId: String) : ViewModel() {
    private val _state = MutableStateFlow(PaymentState())
    val state: StateFlow<PaymentState> = _state.asStateFlow()

    fun pay(outcome: String, onResult: (PaymentVerificationResult) -> Unit) {
        if (_state.value.busy || _state.value.finished) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            c.bookingRepository.verifySimulatedPayment(bookingId, outcome)
                .onSuccess { r -> _state.update { it.copy(busy = false, finished = true) }; onResult(r) }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUserMessage()) } }
        }
    }
}

class ConfirmationViewModel(private val c: AppContainer, private val eventId: String) : ViewModel() {
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    init {
        viewModelScope.launch {
            _event.value = c.eventRepository.fetchEventById(eventId).getOrNull()
            c.goPointsRepository.refreshAchievements()
        }
    }
}

// =========================================================== tickets

data class TicketsState(val loading: Boolean = true, val error: String? = null, val tickets: List<TicketDetail> = emptyList())

class TicketsViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(TicketsState())
    val state: StateFlow<TicketsState> = _state.asStateFlow()

    fun load() {
        val uid = c.authRepository.currentUserId ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            c.bookingRepository.fetchUserTickets(uid)
                .onSuccess { list -> _state.update { it.copy(loading = false, tickets = list) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }
}

data class TicketDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val ticket: TicketDetail? = null,
    val checking: Boolean = false,
    val validation: TicketValidationResult? = null,
    val validationError: String? = null
)

class TicketDetailViewModel(private val c: AppContainer, private val ticketId: String) : ViewModel() {
    private val _state = MutableStateFlow(TicketDetailState())
    val state: StateFlow<TicketDetailState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            c.bookingRepository.fetchTicketById(ticketId)
                .onSuccess { t -> _state.update { it.copy(loading = false, ticket = t) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }

    fun verify() {
        val token = _state.value.ticket?.qrToken ?: return
        _state.update { it.copy(checking = true, validation = null, validationError = null) }
        viewModelScope.launch {
            c.bookingRepository.validateTicket(token)
                .onSuccess { r -> _state.update { it.copy(checking = false, validation = r) } }
                .onFailure { e -> _state.update { it.copy(checking = false, validationError = e.toUserMessage()) } }
        }
    }
}

// =========================================================== saved events

data class SavedState(val loading: Boolean = true, val error: String? = null, val events: List<Event> = emptyList())

class SavedViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(SavedState())
    val state: StateFlow<SavedState> = _state.asStateFlow()

    fun load(ids: Set<String>) {
        viewModelScope.launch {
            c.eventRepository.fetchEventsByIds(ids.toList())
                .onSuccess { list -> _state.update { it.copy(loading = false, error = null, events = list.sortedBy { e -> e.eventDate }) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }
}

// =========================================================== GoPoints, rewards, achievements

data class PointsState(
    val loading: Boolean = true,
    val error: String? = null,
    val history: List<GoPointsHistoryItem> = emptyList(),
    val rewards: List<Reward> = emptyList(),
    val redemptions: List<RewardRedemption> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val userAchievements: List<UserAchievement> = emptyList(),
    val redeeming: Boolean = false,
    val redeemError: String? = null,
    val lastRedemption: RewardRedemptionResult? = null
)

class PointsViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(PointsState())
    val state: StateFlow<PointsState> = _state.asStateFlow()

    fun load(refreshAchievements: Boolean = false) {
        val uid = c.authRepository.currentUserId ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (refreshAchievements) c.goPointsRepository.refreshAchievements()
            val history = async { c.goPointsRepository.fetchPointsHistory(uid) }
            val rewards = async { c.goPointsRepository.fetchRewards() }
            val redemptions = async { c.goPointsRepository.fetchRedemptions(uid) }
            val ach = async { c.goPointsRepository.fetchAchievements() }
            val userAch = async { c.goPointsRepository.fetchUserAchievements(uid) }
            val h = history.await()
            _state.update {
                it.copy(
                    loading = false,
                    error = if (h.isFailure) h.exceptionOrNull()?.toUserMessage() else null,
                    history = h.getOrDefault(emptyList()),
                    rewards = rewards.await().getOrDefault(emptyList()),
                    redemptions = redemptions.await().getOrDefault(emptyList()),
                    achievements = ach.await().getOrDefault(emptyList()),
                    userAchievements = userAch.await().getOrDefault(emptyList())
                )
            }
        }
    }

    fun redeem(reward: Reward, onDone: () -> Unit) {
        if (_state.value.redeeming) return
        _state.update { it.copy(redeeming = true, redeemError = null, lastRedemption = null) }
        viewModelScope.launch {
            c.goPointsRepository.redeemReward(reward.id)
                .onSuccess { r -> _state.update { it.copy(redeeming = false, lastRedemption = r) }; onDone(); load() }
                .onFailure { e -> _state.update { it.copy(redeeming = false, redeemError = e.toUserMessage()) } }
        }
    }

    fun dismissRedemption() = _state.update { it.copy(lastRedemption = null, redeemError = null) }
}

// =========================================================== notifications

data class NotificationsState(val loading: Boolean = true, val error: String? = null, val items: List<NotificationItem> = emptyList())

class NotificationsViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    fun load() {
        val uid = c.authRepository.currentUserId ?: return
        viewModelScope.launch {
            c.notificationRepository.fetchNotifications(uid)
                .onSuccess { list -> _state.update { it.copy(loading = false, error = null, items = list) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }

    fun markRead(id: String) {
        _state.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(isRead = true) else it }) }
        viewModelScope.launch { c.notificationRepository.markAsRead(id) }
    }

    fun markAllRead() {
        val uid = c.authRepository.currentUserId ?: return
        _state.update { s -> s.copy(items = s.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch { c.notificationRepository.markAllAsRead(uid) }
    }
}

// =========================================================== interests

data class InterestsState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val selected: Set<String> = emptySet(),
    val saving: Boolean = false
)

class InterestsViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(InterestsState())
    val state: StateFlow<InterestsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val uid = c.authRepository.currentUserId
            val cats = c.eventRepository.fetchCategories()
            val chosen = uid?.let { c.profileRepository.fetchInterestIds(it).getOrNull() }.orEmpty()
            _state.update {
                it.copy(loading = false, categories = cats.getOrDefault(emptyList()), selected = chosen.toSet(), error = cats.exceptionOrNull()?.toUserMessage())
            }
        }
    }

    fun toggle(id: String) = _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }

    fun save(onDone: () -> Unit) {
        val uid = c.authRepository.currentUserId ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            c.profileRepository.saveInterests(uid, _state.value.selected.toList())
                .onSuccess { _state.update { it.copy(saving = false) }; onDone() }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.toUserMessage()) } }
        }
    }
}
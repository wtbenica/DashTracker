package dev.benica.mileagetracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER
import com.wtb.notificationutil.NotificationUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.parcelize.Parcelize
import java.time.format.DateTimeFormatter

val Any.TAG: String
    get() = "GT_" + this.javaClass.simpleName

@ExperimentalCoroutinesApi
class LocationService : Service() {
    // Private properties
    private val _tripId = MutableStateFlow<Long?>(null)
    private var serviceRunningInForeground = false
    private var configurationChange = false
    private var activityTransitionReceiverRegistered = false
    private var activityUpdateReceiverRegistered = false

    private val _isStarted = MutableStateFlow(false)
    private val _isStill = MutableStateFlow(false)
    private val _inVehicle = MutableStateFlow(false)
    private val _onFoot = MutableStateFlow(false)
    private val _isTesting = MutableStateFlow(false)

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val sharedPrefs
        get() = getSharedPreferences(SHARED_PREFS, 0)

    // State accessors
    /**
     * The current tripId, null if not set. It is the value that is passed to the handler that is
     * passed to [start].
     */
    val tripId: StateFlow<Long?>
        get() = _tripId

    /**
     * The service is running
     */
    private val isStarted: StateFlow<Boolean>
        get() = _isStarted

    /**
     * The user's detected activity is [DetectedActivity.STILL]
     */
    private val isStill: StateFlow<Boolean>
        get() = _isStill

    /**
     * The user's detected activity is [DetectedActivity.IN_VEHICLE]
     */
    private val inVehicle: StateFlow<Boolean>
        get() = _inVehicle

    fun setIsTesting(isTesting: Boolean) {
        _isTesting.value = isTesting
    }

    /**
     * The current [ServiceState] of the [LocationService]. When tracking is started, state is
     * either [ServiceState.TRACKING_ACTIVE] or [ServiceState.PAUSED], else it is
     * [ServiceState.STOPPED].
     */
    val serviceState: StateFlow<ServiceState> =
        combine(isStarted, tripId) { started: Boolean, id: Long? ->
            val res = when {
                started && (id != null) -> ServiceState.TRACKING_ACTIVE
                !started && (id != null) -> ServiceState.PAUSED
                else -> ServiceState.STOPPED
            }

            sharedPrefs.edit().putBoolean(PREFS_IS_PAUSED, res == ServiceState.PAUSED).apply()

            res
        }.stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ServiceState.STOPPED
        )

    // Public methods
    /**
     * Initializes the service with information for creating the ongoing notification that shows
     * when the service is running in the background (as a foreground service)
     *
     * @param notificationData a [NotificationUtils.NotificationData] for creating the ongoing
     * [Notification]
     * @param notificationChannel the [NotificationChannel] of the [Notification]
     * @param notificationText a function to get the initial main text of the [Notification] from
     * the current [Location]. see [Notification.Builder.setContentText]
     * @param updateNotificationText null if the [Notification] text should not update when a new
     * location is received; otherwise a function to process a [Location]
     */
    fun initialize(
        notificationData: NotificationUtils.NotificationData,
        notificationChannel: NotificationChannel,
        notificationText: (Location?) -> String,
        updateNotificationText: ((Location?) -> String)? = null
    ) {
        Log.d(TAG, "EBLOW: Updating notificationData")
        this.nd = notificationData
        this.notificationChannel = notificationChannel
        this.getNotificationText = notificationText
        this.getUpdateNotificationText = updateNotificationText
        notificationUtil.updateNotificationData(notificationData)
    }

    /**
     * Starts location tracking. Sets [serviceState] to [ServiceState.TRACKING_ACTIVE]. It will call
     * [Service.startService] if necessary, and will begin receiving [Location] updates.
     * If [serviceState] was [ServiceState.STOPPED], it sets [tripId] to [newTripId]. If
     * [serviceState] was [ServiceState.PAUSED] or [ServiceState.TRACKING_ACTIVE], [tripId] remains
     * unchanged.
     *
     * @param newTripId the next expected [tripId]
     * @param locationHandler a handler function for incoming [Location] and the current [tripId]
     * @return if the [serviceState] was originally [ServiceState.STOPPED], [newTripId]. If the
     * [serviceState] was [ServiceState.PAUSED] or [ServiceState.TRACKING_ACTIVE], it will return the
     * existing [tripId].
     */
    fun start(
        newTripId: Long,
        locationHandler: (Location, Long, Int, Int, Int, Int) -> Unit
    ): Long {
        Log.d(TAG, "start | called while: ${serviceState.value.name}")
        return when (serviceState.value) {
            ServiceState.STOPPED -> {
                Log.d(TAG, "start | calling startService")
                startService(
                    Intent(applicationContext, LocationService::class.java)
                        .putExtra(EXTRA_LOCATION_HANDLER, LocationHandler(locationHandler))
                        .putExtra(EXTRA_TRIP_ID, newTripId)
                )
                newTripId
            }
            ServiceState.PAUSED -> {
                _isStarted.value = true
                tripId.value!!
            }
            else -> {
                // Do nothing
                tripId.value!!
            }
        }
//
//        return this.tripId.value ?: newTripId
    }

    /**
     * Pauses [Location] updates, which changes [serviceState] to [ServiceState.PAUSED]. If
     * [start] is called again before a call to [stop], [Location] updates will resume using the
     * same [tripId].
     *
     */
    fun pause() {
        _isStarted.value = false
    }

    /**
     * Stops [Location] updates and sets [tripId] to null. The next time [start] is called,
     * [tripId] will be set to the 'newTripId' argument.
     *
     */
    fun stop(id: Long? = null) {
        id?.let { if (it != tripId.value) return }

        userActivity.removeActivityUpdates()
        if (activityUpdateReceiverRegistered) {
            unregisterReceiver(activityUpdateReceiver)
            activityUpdateReceiverRegistered = false
        }

        try {
            val removeTask = locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

            if (removeTask != null) {
                removeTask.addOnCompleteListener {
                    if (it.isSuccessful) {
                        _tripId.value = null
                        _isStarted.value = false
                        stopSelf()
                    }
                }
            } else {
                _tripId.value = null
                _isStarted.value = false
                stopSelf()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.toString())
        }
    }


    private val localBinder = LocalBinder()

    // Notification stuff
    private var nd: NotificationUtils.NotificationData? = null
    private var notificationChannel: NotificationChannel? = null
    private val notificationUtil by lazy {
        NotificationUtils(
            context = this,
            nd = nd ?: throw UninitializedPropertyAccessException(
                getString(
                    R.string.uninit_notif_data_exception
                )
            )
        )
    }
    private var getNotificationText: ((Location?) -> String)? = null
    var getUpdateNotificationText: ((Location?) -> String)? = null

    // Location tracking
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null

    // Activity Detection
    private val userActivity = ActivityTransitionUtils(this)

    private fun getLocationCallback(onComplete: (loc: Location, tripId: Long, still: Int, inCar: Int, onFoot: Int, unknown: Int) -> Unit) =
        object : LocationCallback() {
            override fun onLocationResult(loc: LocationResult) {
                super.onLocationResult(loc)
                val lastLoc = loc.lastLocation
                if (lastLoc == null || !lastLoc.hasAccuracy() || lastLoc.accuracy > 20f ||
                    serviceState.value == ServiceState.PAUSED
                ) {
                    return
                }

                currentLocation = lastLoc
                tripId.value?.let {
                    onComplete(
                        lastLoc,
                        it,
                        stillVal.value,
                        carVal.value,
                        footVal.value,
                        unknownVal.value
                    )
                }

                if (serviceRunningInForeground) {
                    getUpdateNotificationText?.let {
                        notificationUtil.updateNotification(
                            it(currentLocation),
                            NOTIFICATION_ID,
                            notificationChannel
                                ?: throw IllegalStateException(getString(R.string.uninit_notif_data_exception))

                        )
                    }
                }
            }
        }

    override fun onCreate() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        intent?.let {
            Log.d(TAG, "onStartCommand: has intent")

            if (it.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)) {
                stop()
            }

            val locHandler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra(EXTRA_LOCATION_HANDLER, LocationHandler::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra(EXTRA_LOCATION_HANDLER)
            }
            val tripId: Long = it.getLongExtra(EXTRA_TRIP_ID, -1L)

            if (tripId != -1L) {
                _tripId.value = tripId
            }

            _isStarted.value = !sharedPrefs.getBoolean(PREFS_IS_PAUSED, false)

            locHandler?.handleLocation?.let { lh ->
                registerReceiver(
                    activityUpdateReceiver,
                    IntentFilter(ActivityUpdateReceiver.ACT_UPDATE_INTENT)
                )
                activityUpdateReceiverRegistered = true
                userActivity.registerForActivityUpdates()

                try {
                    locationCallback = getLocationCallback(lh)
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback!!,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, e.toString())
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        if (activityUpdateReceiverRegistered) {
            unregisterReceiver(activityUpdateReceiver)
        }

        if (activityTransitionReceiverRegistered) {
            unregisterReceiver(activityTransitionReceiver)
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind")
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceRunningInForeground = false
        configurationChange = false

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        if (!configurationChange) {
            Log.d(TAG, "onUnbind: starting ongoing background notification")
            val notification: Notification = notificationUtil.generateNotification(
                getNotificationText?.invoke(currentLocation) ?: "Location Service in use.",
                notificationChannel
                    ?: throw IllegalStateException(getString(R.string.uninit_notif_data_exception))
            )
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    private val _stillVal = MutableStateFlow(-1)
    val stillVal: StateFlow<Int>
        get() = _stillVal

    private val _carVal = MutableStateFlow(-1)
    val carVal: StateFlow<Int>
        get() = _carVal

    private val _footVal = MutableStateFlow(-1)
    val footVal: StateFlow<Int>
        get() = _footVal

    private val _unknownVal = MutableStateFlow(-1)
    val unknownVal: StateFlow<Int>
        get() = _unknownVal

    private val activityUpdateReceiver = ActivityUpdateReceiver().apply {
        action = { result ->
            val still = result.getActivityConfidence(DetectedActivity.STILL)
            val car = result.getActivityConfidence(DetectedActivity.IN_VEHICLE)
            val foot = result.getActivityConfidence(DetectedActivity.ON_FOOT)
            val unknown = result.getActivityConfidence(DetectedActivity.UNKNOWN)

            _stillVal.value = still
            _carVal.value = car
            _footVal.value = foot
            _unknownVal.value = unknown

            _isStill.value = still > 50
            _inVehicle.value = car > 50
            _onFoot.value = foot > 50

//            if (unknown < 10 && (car > 70 || foot > 70 || still > 70)) {
//                unregisterReceiver(this)
//                activityUpdateReceiverRegistered = false
//                userActivity.removeActivityUpdates()
//
//                registerReceiver(
//                    activityTransitionReceiver,
//                    IntentFilter(ActivityTransitionReceiver.ACT_TRANS_INTENT)
//                )
//                userActivity.registerForActivityTransitionUpdates()
//                activityTransitionReceiverRegistered = true
//            }
        }
    }

    private val activityTransitionReceiver = ActivityTransitionReceiver().apply {
        action = { event ->
            when (event.activityType) {
                DetectedActivity.STILL -> {
                    _isStill.value = event.transitionType == ACTIVITY_TRANSITION_ENTER
                }
                DetectedActivity.IN_VEHICLE -> {
                    _inVehicle.value = event.transitionType == ACTIVITY_TRANSITION_ENTER
                }
                DetectedActivity.ON_FOOT -> {
                    _onFoot.value = event.transitionType == ACTIVITY_TRANSITION_ENTER
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        val service: LocationService
            get() = this@LocationService
    }

    enum class ServiceState {
        TRACKING_ACTIVE, TRACKING_INACTIVE, PAUSED, STOPPED
    }

    @Parcelize
    class LocationHandler(
        val handleLocation: (loc: Location, tripId: Long, still: Int, inCar: Int, onFoot: Int, unknown: Int) -> Unit
    ) : Parcelable

    companion object {
        private const val NOTIFICATION_ID = 101101
        private const val UPDATE_INTERVAL_MILLISECONDS: Long = 5000
        private const val FASTEST_UPDATE_INTERVAL_MILLISECONDS = UPDATE_INTERVAL_MILLISECONDS / 2
        private const val PREFS_IS_PAUSED = "service_state"
        const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "${BuildConfig.LIBRARY_PACKAGE_NAME}.extra.EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICAITON"

        const val SHARED_PREFS = "mileage_tracker"
        const val EXTRA_LOCATION_HANDLER = "${BuildConfig.LIBRARY_PACKAGE_NAME}.locHandler"
        const val EXTRA_TRIP_ID = "${BuildConfig.LIBRARY_PACKAGE_NAME}.tripId"

        private val locationRequest: LocationRequest
            get() = LocationRequest.create().apply {
                interval = UPDATE_INTERVAL_MILLISECONDS
                fastestInterval = FASTEST_UPDATE_INTERVAL_MILLISECONDS
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }

        var dtf: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss:SSS")

    }
}


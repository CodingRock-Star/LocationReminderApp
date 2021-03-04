package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SaveReminderFragment : BaseFragment() {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val GEOFENCE_RADIUS_VALUE = 600f
    private lateinit var pendingIntent: PendingIntent

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        geofencingClient = LocationServices.getGeofencingClient(context!!)
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val geofenceId = UUID.randomUUID().toString()

            if (latitude != null && longitude != null && !TextUtils.isEmpty(title))
                addToGeofenceClue(LatLng(latitude, longitude), GEOFENCE_RADIUS_VALUE, geofenceId)

            _viewModel.validateAndSaveReminder(
                ReminderDataItem(
                    title,
                    description,
                    location,
                    latitude,
                    longitude
                )
            )

            _viewModel.navigateToReminderList.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    if (it) {
                        view.findNavController()
                            .navigate(R.id.action_saveReminderFragment_to_reminderListFragment)
                        _viewModel.navigateToReminderList()
                    }
                })

        }
    }

    override fun onDestroy() {
        super.onDestroy()
       // removeGeoFence()
        _viewModel.onClear()

    }

    @SuppressLint("MissingPermission")
    private fun addToGeofenceClue(
        latLng: LatLng,
        radius: Float,
        Id: String
    ) {
        val geofence: Geofence = getGeofenceBuilder(
            Id,
            latLng,
            radius,
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        )
        val request: GeofencingRequest = getGeofenceBuilderRequest(geofence)
        val pendingIntent: PendingIntent? = getGeofencePendingIntent()
        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener(OnSuccessListener<Void?> {
                Toast.makeText(
                    context,
                    resources.getString(R.string.geofences_added),
                    Toast.LENGTH_LONG
                ).show()
            })
            .addOnFailureListener(OnFailureListener { e ->
                val errorMessage: String = getErrorString(e)
                Toast.makeText(
                    context,
                    resources.getString(R.string.permissions_request),
                    Toast.LENGTH_LONG
                ).show()
            })
    }


    private fun getGeofencePendingIntent(): PendingIntent? {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        pendingIntent =
            PendingIntent.getBroadcast(
                activity,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        return pendingIntent
    }

    private fun getErrorString(e: Exception): String {
        if (e is ApiException) {
            when (e.statusCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> return getString(
                    R.string.geofence_not_available
                )
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> return getString(
                    R.string.geofence_too_many_geofences
                )
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> return getString(
                    R.string.geofence_too_many_pending_intents
                )
            }
        }
        return e.message.toString()
    }

    private fun getGeofenceBuilderRequest(geofence: Geofence?): GeofencingRequest {
        return GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()
    }

    private fun getGeofenceBuilder(
        ID: String,
        latLng: LatLng,
        radius: Float,
        transitionTypes: Int
    ): Geofence {
        return Geofence.Builder()
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setRequestId(ID)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(DELAY)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }

     private fun removeGeoFence(){
         geofencingClient.removeGeofences(getGeofencePendingIntent())?.run {
             addOnSuccessListener {
                 Toast.makeText(context,context?.getString(R.string.geofences_removed),Toast.LENGTH_LONG).show()
             }
             addOnFailureListener{
                 Toast.makeText(context,context?.getString(R.string.geofences_not_removed),Toast.LENGTH_LONG).show()
             }
         }
     }
    companion object {
        const val REQUEST_CODE: Int = 1207
        private const val DELAY: Int = 5000
        private val TAG = SaveReminderFragment::class.java.simpleName
    }
}
package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var mapMarker: Marker
    private var currentPoint: PointOfInterest? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastLocationSaved: Location? = null
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    companion object {
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 1002
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val ZOOM = 15
        private val defaultLocation = LatLng(-14.921499703661087, -40.209044831600515)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)



        val supportFragmentManager = requireActivity().supportFragmentManager
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.map_fragment, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.saveButton.setOnClickListener {
            getLocationSelected()
        }

        return binding.root
    }

    private fun getLocationSelected() {
        if (currentPoint != null) {
            Log.i("currentPoint"," ENTER!!!! ")
            _viewModel.latitude.value = currentPoint?.latLng?.latitude
            _viewModel.longitude.value = currentPoint?.latLng?.longitude
            _viewModel.reminderSelectedLocationStr.value = currentPoint?.name
            _viewModel.selectedPOI.value = currentPoint
            findNavController().navigateUp()
        } else if (mapMarker != null) {
            Log.i("MapMarker"," ENTER!!!! ")
            _viewModel.latitude.value = mapMarker.position?.latitude
            _viewModel.longitude.value = mapMarker.position?.longitude
            _viewModel.reminderSelectedLocationStr.value = mapMarker.title
            _viewModel.selectedPOI.value = currentPoint
            findNavController().navigateUp()
        }
        else {
            Toast.makeText(context, "Please select a point of interest for the reminder", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (_viewModel.selectedPOI.value != null) {
            currentPoint = _viewModel.selectedPOI.value
            mapMarker = map.addMarker(
                MarkerOptions()
                    .position(_viewModel.selectedPOI.value!!.latLng)
                    .title(_viewModel.selectedPOI.value!!.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            map.moveCamera(CameraUpdateFactory.newLatLng(_viewModel.selectedPOI.value!!.latLng))
        }
        else {
            currentPoint = PointOfInterest(defaultLocation, "default", "default")
            mapMarker = map.addMarker(
                MarkerOptions()
                    .position(defaultLocation)
                    .title("Default Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            map.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation))

        }

        setMapStyle(map)
        setMapLongClick(map)
        enableMyLocation()
        onLocationSelected()
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            binding.saveButton.isEnabled = true
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            currentPoint = null
            mapMarker.remove()
            mapMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Lat: %1$.5f, Long: %2$.5f".format(latLng.latitude, latLng.longitude))
                    .snippet(snippet)
            )!!
            Log.i("setMapLongClick"," ENTER!!!! ${mapMarker.position}")

            map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    private fun onLocationSelected() {
        map.setOnPoiClickListener { pointOfInterest ->
            val currentLocation = pointOfInterest.latLng
            binding.saveButton.isEnabled = true
            mapMarker.remove()

            currentPoint = pointOfInterest
            mapMarker = map.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title(pointOfInterest.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            map.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))

        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            updateUI()
            getDeviceLocation(true)
        }else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                R.string.location_required_error,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(android.R.string.ok) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }.show()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_PERMISSION ) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                map.isMyLocationEnabled = true
                enableMyLocation()
            }
            else {
                Snackbar.make(
                    requireView(),
                    "We need your location permission to set your location in the map",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
        else {
            Snackbar.make(
                requireView(),
                "We need your location permission to set your location in the map",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }


    private fun updateUI() {
        try {
            if (isPermissionGranted()) {
                setMapStyle(map)
                map.uiSettings?.isMyLocationButtonEnabled = true
                map.uiSettings?.isMapToolbarEnabled = false
                map.isMyLocationEnabled = true
            } else {
                map.uiSettings?.isMyLocationButtonEnabled = false
                map.uiSettings?.isMapToolbarEnabled = false
                map.isMyLocationEnabled = false
                lastLocationSaved = null
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(resolve: Boolean) {
             val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                startIntentSenderForResult(
                    exception.resolution.intentSender,
                    REQUEST_TURN_DEVICE_LOCATION_ON,
                    null,
                    0,
                    0,
                    0,
                    null
                )

            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    getDeviceLocation(true)
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val zoomLevel = 15f
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    currentLatLng,
                                    zoomLevel
                                )
                            )
                        }
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_TURN_DEVICE_LOCATION_ON -> {
                updateUI()
                getDeviceLocation(false)
            }
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        val tag = SelectLocationFragment::class.java.simpleName
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(tag, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(tag, "Can't find style. Error: ", e)
        }
    }
 }

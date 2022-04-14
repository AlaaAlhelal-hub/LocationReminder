package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.google.android.gms.common.api.ApiException
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
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var mapMarker: Marker
    private var currentPoint: PointOfInterest? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastLocationSaved: Location? = null

    companion object {
        private val REQUEST_LOCATION_PERMISSION = 1
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
            _viewModel.latitude.postValue(mapMarker?.position?.latitude)
            _viewModel.longitude.postValue(mapMarker?.position?.longitude)
            _viewModel.selectedPOI.postValue(currentPoint)
            _viewModel.reminderSelectedLocationStr.postValue(currentPoint?.name)
            _viewModel.navigationCommand.postValue(NavigationCommand.Back)
        }

        return binding.root
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
        enableMyLocation()
        onLocationSelected()
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
        ) === PackageManager.PERMISSION_GRANTED
    }



    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
            updateUI()
            getDeviceLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }





    private fun updateUI() {
        try {
            if (isPermissionGranted()) {
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

    private fun getDeviceLocation() {
        try {
            if (isPermissionGranted()) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        lastLocationSaved = task.result!!
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    lastLocationSaved!!.latitude,
                                    lastLocationSaved!!.longitude
                                ), ZOOM.toFloat()
                            )
                        )
                    } else {
                        map.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, ZOOM.toFloat())
                        )
                        map.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }


}
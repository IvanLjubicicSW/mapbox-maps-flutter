package com.mapbox.maps.mapbox_maps

import android.content.Context
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.mapbox.common.*
import com.mapbox.maps.*
import com.mapbox.maps.mapbox_maps.annotation.AnnotationController
import com.mapbox.maps.pigeons.FLTMapInterfaces
import com.mapbox.maps.pigeons.FLTSettings
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class MapboxMapController(
  context: Context,
  mapInitOptions: MapInitOptions,
  private val lifecycleProvider: MapboxMapsPlugin.LifecycleProvider,
  eventTypes: List<Int>,
  messenger: BinaryMessenger,
  channelSuffix: Int,
  pluginVersion: String
) : PlatformView,
  DefaultLifecycleObserver,
  MethodChannel.MethodCallHandler {

  private val mapView: MapView = MapView(context, mapInitOptions)
  private val mapboxMap: MapboxMap = mapView.mapboxMap
  private val methodChannel: MethodChannel
  private val styleController: StyleController = StyleController(mapboxMap, context)
  private val cameraController: CameraController = CameraController(mapboxMap, context)
  private val projectionController: MapProjectionController = MapProjectionController(mapboxMap)
  private val mapInterfaceController: MapInterfaceController = MapInterfaceController(mapboxMap, context)
  private val animationController: AnimationController = AnimationController(mapboxMap, context)
  private val annotationController: AnnotationController = AnnotationController(mapView, mapboxMap)
  private val locationComponentController = LocationComponentController(mapView, context)
  private val gestureController = GestureController(mapView)
  private val logoController = LogoController(mapView)
  private val attributionController = AttributionController(mapView)
  private val scaleBarController = ScaleBarController(mapView)
  private val compassController = CompassController(mapView)

  private val proxyBinaryMessenger = ProxyBinaryMessenger(messenger, "/map_$channelSuffix")
  init {
    changeUserAgent(pluginVersion)
    lifecycleProvider.getLifecycle()?.addObserver(this)
    FLTMapInterfaces.StyleManager.setup(proxyBinaryMessenger, styleController)
    FLTMapInterfaces._CameraManager.setup(proxyBinaryMessenger, cameraController)
    FLTMapInterfaces.Projection.setup(proxyBinaryMessenger, projectionController)
    FLTMapInterfaces._MapInterface.setup(proxyBinaryMessenger, mapInterfaceController)
    FLTMapInterfaces._AnimationManager.setup(proxyBinaryMessenger, animationController)
    annotationController.setup(proxyBinaryMessenger)
    FLTSettings.LocationComponentSettingsInterface.setup(proxyBinaryMessenger, locationComponentController)
    FLTSettings.LogoSettingsInterface.setup(proxyBinaryMessenger, logoController)
    FLTSettings.GesturesSettingsInterface.setup(proxyBinaryMessenger, gestureController)
    FLTSettings.AttributionSettingsInterface.setup(proxyBinaryMessenger, attributionController)
    FLTSettings.ScaleBarSettingsInterface.setup(proxyBinaryMessenger, scaleBarController)
    FLTSettings.CompassSettingsInterface.setup(proxyBinaryMessenger, compassController)

    methodChannel = MethodChannel(proxyBinaryMessenger, "plugins.flutter.io")
    methodChannel.setMethodCallHandler(this)

    // TODO: check if state-triggered subscription change does not lead to multiple subscriptions/not unsubscribing when listener becomes null
    for (event in eventTypes) {
      subscribeToEvent(FLTMapInterfaces._MapEvent.values()[event])
    }
  }

  private fun subscribeToEvent(event: FLTMapInterfaces._MapEvent) {
    // check deserialization of these events, as they are separate structs declared in GL Native and Flutter plugin
    when (event) {
      FLTMapInterfaces._MapEvent.MAP_LOADED -> mapboxMap.subscribeMapLoaded {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.MAP_LOADING_ERROR -> mapboxMap.subscribeMapLoadingError {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.STYLE_LOADED -> mapboxMap.subscribeStyleLoaded {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.STYLE_DATA_LOADED -> mapboxMap.subscribeStyleDataLoaded {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.CAMERA_CHANGED -> mapboxMap.subscribeCameraChanged {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.MAP_IDLE -> mapboxMap.subscribeMapIdle {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.SOURCE_ADDED -> mapboxMap.subscribeSourceAdded {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.SOURCE_REMOVED -> mapboxMap.subscribeSourceRemoved {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.SOURCE_DATA_LOADED -> mapboxMap.subscribeSourceDataLoaded {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.STYLE_IMAGE_MISSING -> mapboxMap.subscribeStyleImageMissing {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.STYLE_IMAGE_REMOVE_UNUSED -> mapboxMap.subscribeStyleImageRemoveUnused {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.RENDER_FRAME_STARTED -> mapboxMap.subscribeRenderFrameStarted {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.RENDER_FRAME_FINISHED -> mapboxMap.subscribeRenderFrameFinished {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
      FLTMapInterfaces._MapEvent.RESOURCE_REQUEST -> mapboxMap.subscribeResourceRequest {
        methodChannel.invokeMethod(event.methodName, Gson().toJson(it))
      }
    }
  }

  override fun getView(): View {
    return mapView
  }

  override fun dispose() {
    lifecycleProvider.getLifecycle()?.removeObserver(this)
    mapView.onStop()
    mapView.onDestroy()
    methodChannel.setMethodCallHandler(null)
    FLTMapInterfaces.StyleManager.setup(proxyBinaryMessenger, null)
    FLTMapInterfaces._CameraManager.setup(proxyBinaryMessenger, null)
    FLTMapInterfaces.Projection.setup(proxyBinaryMessenger, null)
    FLTMapInterfaces._MapInterface.setup(proxyBinaryMessenger, null)
    FLTMapInterfaces._AnimationManager.setup(proxyBinaryMessenger, null)
    annotationController.dispose(proxyBinaryMessenger)
    FLTSettings.LocationComponentSettingsInterface.setup(proxyBinaryMessenger, null)
    FLTSettings.LogoSettingsInterface.setup(proxyBinaryMessenger, null)
    FLTSettings.GesturesSettingsInterface.setup(proxyBinaryMessenger, null)
    FLTSettings.CompassSettingsInterface.setup(proxyBinaryMessenger, null)
    FLTSettings.ScaleBarSettingsInterface.setup(proxyBinaryMessenger, null)
    FLTSettings.AttributionSettingsInterface.setup(proxyBinaryMessenger, null)
  }

  override fun onStart(owner: LifecycleOwner) {
    super.onStart(owner)
    mapView.onStart()
  }

  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
    mapView.onStop()
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      "annotation#create_manager" -> {
        annotationController.handleCreateManager(call, result)
      }
      "annotation#remove_manager" -> {
        annotationController.handleRemoveManager(call, result)
      }
      "gesture#add_listeners" -> {
        gestureController.addListeners(proxyBinaryMessenger)
        result.success(null)
      }
      "gesture#remove_listeners" -> {
        gestureController.removeListeners()
        result.success(null)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun changeUserAgent(version: String) {
    HttpServiceFactory.setHttpServiceInterceptor(
      object : HttpServiceInterceptorInterface {
        override fun onRequest(
          request: HttpRequest,
          continuation: HttpServiceInterceptorRequestContinuation
        ) {
          request.headers["user-agent"] = "${request.headers["user-agent"]} Flutter Plugin/$version"
          continuation.run(HttpRequestOrResponse(request))
        }

        override fun onResponse(
          response: HttpResponse,
          continuation: HttpServiceInterceptorResponseContinuation
        ) {
          continuation.run(response)
        }
      }
    )
  }
}

private val FLTMapInterfaces._MapEvent.methodName: String
  get() = "event#$ordinal"
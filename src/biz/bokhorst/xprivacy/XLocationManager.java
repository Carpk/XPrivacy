package biz.bokhorst.xprivacy;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import android.content.Context;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import android.location.LocationListener;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import static de.robv.android.xposed.XposedHelpers.findField;

public class XLocationManager extends XHook {

	public XLocationManager(String methodName, String restrictionName, String[] permissions) {
		super(methodName, restrictionName, permissions);
	}

	// @formatter:off

	// public boolean addNmeaListener(GpsStatus.NmeaListener listener)
	// public void addProximityAlert(double latitude, double longitude, float radius, long expiration, PendingIntent intent)
	// public Location getLastKnownLocation(String provider)
	// public void requestLocationUpdates(String provider, long minTime, float minDistance, LocationListener listener)
	// public void requestLocationUpdates(String provider, long minTime, float minDistance, LocationListener listener, Looper looper)
	// public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria, LocationListener listener, Looper looper)
	// public void requestLocationUpdates(String provider, long minTime, float minDistance, PendingIntent intent)
	// public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria, PendingIntent intent)
	// public void requestSingleUpdate(String provider, LocationListener listener, Looper looper)
	// public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper)
	// public void requestSingleUpdate(String provider, PendingIntent intent)
	// public void requestSingleUpdate(Criteria criteria, PendingIntent intent)
	// frameworks/base/location/java/android/location/LocationManager.java

	// @formatter:on

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		String methodName = param.method.getName();
		if (!methodName.equals("getLastKnownLocation"))
			if (isRestricted(param))
				if (methodName.equals("addNmeaListener")) {
					// addNmeaListener
					NmeaListener nmeaListener = (NmeaListener) param.args[0];
					param.args[0] = new XNmeaListener(nmeaListener);
					XUtil.log(this, Log.INFO, "Replacing NMEA listener");
				} else if (methodName.equals("requestLocationUpdates")) {
					// requestLocationUpdates
					if (param.args[3] != null && param.args[3].getClass().isAssignableFrom(LocationListener.class)) {
						XUtil.log(this, Log.INFO, "Replacing location listener");
						LocationListener listener = (LocationListener) param.args[3];
						param.args[3] = new XLocationListener(listener);
					} else
						param.setResult(null);
				} else if (methodName.equals("requestSingleUpdate")) {
					// requestSingleUpdate
					if (param.args[1] != null && param.args[1].getClass().isAssignableFrom(LocationListener.class)) {
						XUtil.log(this, Log.INFO, "Replacing location listener");
						LocationListener listener = (LocationListener) param.args[1];
						param.args[1] = new XLocationListener(listener);
					} else
						param.setResult(null);
				}
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		if (param.method.getName().equals("getLastKnownLocation"))
			if (param.getResultOrThrowable() != null)
				if (isRestricted(param)) {
					String provider = (String) param.args[0];
					param.setResult(getRandomLocation(provider));
				}
	}

	@Override
	protected boolean isRestricted(MethodHookParam param) throws Throwable {
		Field fieldContext = findField(param.thisObject.getClass(), "mContext");
		Context context = (Context) fieldContext.get(param.thisObject);
		int uid = Binder.getCallingUid();
		return getRestricted(context, uid, true);
	}

	private Location getRandomLocation(String provider) {
		Location location = new Location(provider);
		location.setLatitude(getRandomLat());
		location.setLongitude(getRandomLon());
		return location;
	}

	private double getRandomLat() {
		double lat = Math.random() * 180;
		BigDecimal latitude = new BigDecimal(lat > 90 ? lat - 90 : -lat);
		return latitude.setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	private double getRandomLon() {
		double lon = Math.random() * 360;
		BigDecimal longitude = new BigDecimal(lon > 180 ? lon - 180 : -lon);
		return longitude.setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	private class XNmeaListener implements NmeaListener {

		private NmeaListener mNmeaListener;

		public XNmeaListener(NmeaListener nmeaListener) {
			mNmeaListener = nmeaListener;
		}

		@Override
		public void onNmeaReceived(long timestamp, String nmea) {
			XUtil.log(null, Log.INFO, nmea);
			mNmeaListener.onNmeaReceived(timestamp, nmea);
		}
	}

	private class XLocationListener implements LocationListener {

		private LocationListener mLocationListener;

		public XLocationListener(LocationListener locationListener) {
			mLocationListener = locationListener;
		}

		@Override
		public void onLocationChanged(Location location) {
			mLocationListener.onLocationChanged(getRandomLocation(location.getProvider()));
		}

		@Override
		public void onProviderDisabled(String provider) {
			mLocationListener.onProviderDisabled(provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			mLocationListener.onProviderEnabled(provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			mLocationListener.onStatusChanged(provider, status, extras);
		}
	}
}

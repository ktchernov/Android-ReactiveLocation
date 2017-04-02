package pl.charmas.android.reactivelocation;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.support.annotation.RequiresPermission;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import pl.charmas.android.reactivelocation.observables.GoogleAPIClientObservable;
import pl.charmas.android.reactivelocation.observables.PendingResultObservable;
import pl.charmas.android.reactivelocation.observables.location.AddLocationIntentUpdatesObservable;
import pl.charmas.android.reactivelocation.observables.location.LastKnownLocationObservable;
import pl.charmas.android.reactivelocation.observables.location.LocationUpdatesObservable;
import pl.charmas.android.reactivelocation.observables.location.MockLocationObservable;
import pl.charmas.android.reactivelocation.observables.location.RemoveLocationIntentUpdatesObservable;
import rx.Observable;
import rx.functions.Func1;


/**
 * Factory of observables that can manipulate location
 * delivered by Google Play Services.
 */
public class ReactiveLocationProvider {
    private final Context ctx;

    public ReactiveLocationProvider(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Creates observable that obtains last known location and than completes.
     * Delivered location is never null - when it is unavailable Observable completes without emitting
     * any value.
     * <p/>
     * Observable can report {@link pl.charmas.android.reactivelocation.observables.GoogleAPIConnectionException}
     * when there are trouble connecting with Google Play Services and other exceptions that
     * can be thrown on {@link com.google.android.gms.location.FusedLocationProviderApi#getLastLocation(com.google.android.gms.common.api.GoogleApiClient)}.
     * Everything is delivered by {@link rx.Observer#onError(Throwable)}.
     *
     * @return observable that serves last know location
     */
    @RequiresPermission(anyOf = {
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"
    })
    public Observable<Location> getLastKnownLocation() {
        return LastKnownLocationObservable.createObservable(ctx);
    }

    /**
     * Creates observable that allows to observe infinite stream of location updates.
     * To stop the stream you have to unsubscribe from observable - location updates are
     * then disconnected.
     * <p/>
     * Observable can report {@link pl.charmas.android.reactivelocation.observables.GoogleAPIConnectionException}
     * when there are trouble connecting with Google Play Services and other exceptions that
     * can be thrown on {@link com.google.android.gms.location.FusedLocationProviderApi#requestLocationUpdates(com.google.android.gms.common.api.GoogleApiClient, com.google.android.gms.location.LocationRequest, com.google.android.gms.location.LocationListener)}.
     * Everything is delivered by {@link rx.Observer#onError(Throwable)}.
     *
     * @param locationRequest request object with info about what kind of location you need
     * @return observable that serves infinite stream of location updates
     */
    @RequiresPermission(anyOf = {
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"
    })
    public Observable<Location> getUpdatedLocation(LocationRequest locationRequest) {
        return LocationUpdatesObservable.createObservable(ctx, locationRequest);
    }

    /**
     * Returns an observable which activates mock location mode when subscribed to, using the
     * supplied observable as a source of mock locations. Mock locations will replace normal
     * location information for all users of the FusedLocationProvider API on the device while this
     * observable is subscribed to.
     * <p/>
     * To use this method, mock locations must be enabled in developer options and your application
     * must hold the android.permission.ACCESS_MOCK_LOCATION permission, or a {@link java.lang.SecurityException}
     * will be thrown.
     * <p/>
     * All statuses that are not successful will be reported as {@link pl.charmas.android.reactivelocation.observables.StatusException}.
     * <p/>
     * Every exception is delivered by {@link rx.Observer#onError(Throwable)}.
     *
     * @param sourceLocationObservable observable that emits {@link android.location.Location} instances suitable to use as mock locations
     * @return observable that emits {@link com.google.android.gms.common.api.Status}
     */
    @RequiresPermission(anyOf = {
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"
    })
    public Observable<Status> mockLocation(Observable<Location> sourceLocationObservable) {
        return MockLocationObservable.createObservable(ctx, sourceLocationObservable);
    }

    /**
     * Creates an observable that adds a {@link android.app.PendingIntent} as a location listener.
     * <p/>
     * This invokes {@link com.google.android.gms.location.FusedLocationProviderApi#requestLocationUpdates(com.google.android.gms.common.api.GoogleApiClient, com.google.android.gms.location.LocationRequest, android.app.PendingIntent)}.
     * <p/>
     * When location updates are no longer required, a call to {@link #removeLocationUpdates(android.app.PendingIntent)}
     * should be made.
     * <p/>
     * In case of unsuccessful status {@link pl.charmas.android.reactivelocation.observables.StatusException} is delivered.
     *
     * @param locationRequest request object with info about what kind of location you need
     * @param intent          PendingIntent that will be called with location updates
     * @return observable that adds the request and PendingIntent
     */
    @RequiresPermission(anyOf = {
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"
    })
    public Observable<Status> requestLocationUpdates(LocationRequest locationRequest, PendingIntent intent) {
        return AddLocationIntentUpdatesObservable.createObservable(ctx, locationRequest, intent);
    }

    /**
     * Observable that can be used to remove {@link android.app.PendingIntent} location updates.
     * <p/>
     * In case of unsuccessful status {@link pl.charmas.android.reactivelocation.observables.StatusException} is delivered.
     *
     * @param intent PendingIntent to remove location updates for
     * @return observable that removes the PendingIntent
     */
    public Observable<Status> removeLocationUpdates(PendingIntent intent) {
        return RemoveLocationIntentUpdatesObservable.createObservable(ctx, intent);
    }

    /**
     * Observable that can be used to check settings state for given location request.
     *
     * @param locationRequest location request
     * @return observable that emits check result of location settings
     * @see com.google.android.gms.location.SettingsApi
     */
    public Observable<LocationSettingsResult> checkLocationSettings(final LocationSettingsRequest locationRequest) {
        return getGoogleApiClientObservable(LocationServices.API)
                .flatMap(new Func1<GoogleApiClient, Observable<LocationSettingsResult>>() {
                    @Override
                    public Observable<LocationSettingsResult> call(GoogleApiClient googleApiClient) {
                        return fromPendingResult(LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationRequest));
                    }
                });
    }

    /**
     * Observable that emits {@link com.google.android.gms.common.api.GoogleApiClient} object after connection.
     * In case of error {@link pl.charmas.android.reactivelocation.observables.GoogleAPIConnectionException} is emmited.
     * When connection to Google Play Services is suspended {@link pl.charmas.android.reactivelocation.observables.GoogleAPIConnectionSuspendedException}
     * is emitted as error.
     * Do not disconnect from apis client manually - just unsubscribe.
     *
     * @param apis collection of apis to connect to
     * @return observable that emits apis client after successful connection
     */
    public Observable<GoogleApiClient> getGoogleApiClientObservable(Api... apis) {
        //noinspection unchecked
        return GoogleAPIClientObservable.create(ctx, apis);
    }

    /**
     * Util method that wraps {@link com.google.android.gms.common.api.PendingResult} in Observable.
     *
     * @param result pending result to wrap
     * @param <T>    parameter type of result
     * @return observable that emits pending result and completes
     */
    public static <T extends Result> Observable<T> fromPendingResult(PendingResult<T> result) {
        return Observable.create(new PendingResultObservable<>(result));
    }
}

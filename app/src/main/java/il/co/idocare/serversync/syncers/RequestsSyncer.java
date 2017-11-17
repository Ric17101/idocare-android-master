package il.co.idocare.serversync.syncers;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import il.co.idocare.Constants;
import il.co.idocare.contentproviders.TransactionsController;
import il.co.idocare.location.ReverseGeocoder;
import il.co.idocare.networking.NetworkingUtils;
import il.co.idocare.networking.ServerApi;
import il.co.idocare.networking.schemes.responses.RequestResponseScheme;
import il.co.idocare.networking.schemes.responses.RequestsResponseScheme;
import il.co.idocare.networking.schemes.responses.RequestScheme;
import il.co.idocare.requests.RequestEntity;
import il.co.idocare.requests.events.RequestIdChangedEvent;
import il.co.idocare.requests.events.RequestsChangedEvent;
import il.co.idocare.requests.cachers.RequestsCacher;
import il.co.idocare.requests.cachers.TempIdCacher;
import il.co.idocare.requests.retrievers.RawRequestRetriever;
import il.co.idocare.serversync.SyncFailedException;
import il.co.idocare.utils.Logger;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * This class handles synchronization of requests to/from the server.<br>
 * NOTE: although methods in this class should be called from background threads, the
 * implementation is not thread-safe.
 */
public class RequestsSyncer {

    private static final String TAG = "RequestsSyncer";

    private final RequestsCacher mRequestsCacher;
    private final RawRequestRetriever mRawRequestsRetriever;
    private final TransactionsController mTransactionsController;
    private final TempIdCacher mTempIdCacher;
    private final ServerApi mServerApi;
    private final ReverseGeocoder mReverseGeocoder;
    private final EventBus mEventBus;
    private Logger mLogger;

    public RequestsSyncer(RequestsCacher requestsCacher,
                          RawRequestRetriever rawRequestsRetriever,
                          TransactionsController transactionsController,
                          TempIdCacher tempIdCacher,
                          ServerApi serverApi,
                          ReverseGeocoder reverseGeocoder,
                          EventBus eventBus,
                          Logger logger) {
        mRequestsCacher = requestsCacher;
        mRawRequestsRetriever = rawRequestsRetriever;
        mTransactionsController = transactionsController;
        mTempIdCacher = tempIdCacher;
        mServerApi = serverApi;
        mReverseGeocoder = reverseGeocoder;
        mEventBus = eventBus;
        mLogger = logger;
    }

    @WorkerThread
    public void syncRequestCreated(String requestId) {
        mLogger.d(TAG, "syncRequestCreated(); request ID: " + requestId);


        RequestEntity request = mRawRequestsRetriever.getRequestById(requestId);

        MultipartBody.Builder builder = new MultipartBody.Builder();

        builder.setType(MultipartBody.FORM);

        builder.addFormDataPart(Constants.FIELD_NAME_CREATED_COMMENT, request.getCreatedComment());
        builder.addFormDataPart(Constants.FIELD_NAME_LATITUDE, String.valueOf(request.getLatitude()));
        builder.addFormDataPart(Constants.FIELD_NAME_LONGITUDE, String.valueOf(request.getLongitude()));
        builder.addFormDataPart(Constants.FIELD_NAME_CREATED_POLLUTION_LEVEL, "1"); // TODO: remove


        builder = NetworkingUtils.addPicturesParts(
                builder,
                request.getCreatedPictures(),
                Constants.FIELD_NAME_CREATED_PICTURES
        );

        Call<RequestResponseScheme> call = mServerApi.createNewRequest(builder.build());

        try {
            Response<RequestResponseScheme> response = call.execute();

            if (response.isSuccessful()) {
                RequestEntity updatedRequest = convertSchemeToRequest(response.body().getRequestScheme());
                // update temp request with new information
                mRequestsCacher.updateWithPossibleIdChange(updatedRequest, requestId);
                // cache the mapping from temp request ID to a new one
                mTempIdCacher.cacheTempIdMapping(requestId, updatedRequest.getId());
                // post event that notifies about ID change
                mEventBus.post(new RequestIdChangedEvent(requestId, updatedRequest.getId()));
            } else {
                throw new SyncFailedException("create new request call failed; response code: " + response.code());
            }

        } catch (IOException e) {
            throw new SyncFailedException(e);
        }
    }


    @WorkerThread
    public void syncAllRequests() {
        mLogger.d(TAG, "syncAllRequests()");

        Call<RequestsResponseScheme> call = mServerApi.getRequests();

        try {
            Response<RequestsResponseScheme> response = call.execute();

            if (response.isSuccessful()) {
                long startProcessingTime = System.currentTimeMillis();
                processResponse(response.body().getRequestSchemes());
                long totalProcessingTime = System.currentTimeMillis() - startProcessingTime;
                mLogger.v(TAG, "processing of all requests took: " + totalProcessingTime + " ms");
            } else {
                throw new SyncFailedException("couldn't fetch requests from the server; response code: " + response.code());
            }
        } catch (IOException e) {
            throw new SyncFailedException(e);
        }
    }

    private void processResponse(@Nullable List<RequestScheme> requests) {
        mLogger.d(TAG, "processResponse() called");

        mTransactionsController.beginTransaction();

        try {

            if (requests == null || requests.isEmpty()) {
                mLogger.d(TAG, "empty request list - deleting all requests from local cache");
                mRequestsCacher.deleteAllRequests();
            } else {
                List<String> processedRequestIds = updateOrInsertRequestSchemes(requests);

                // currently we assume that the requests not returned by the server can be deleted
                // TODO: review this assumption
                mRequestsCacher.deleteAllRequestsWithNonMatchingIds(processedRequestIds);
            }
            mTransactionsController.setTransactionSuccessful();

        } finally {
            mTransactionsController.endTransaction();
        }

        notifyRequestsChanged();
    }

    /**
     * @return list containing IDs of all processed requests
     */
    private List<String> updateOrInsertRequestSchemes(List<RequestScheme> requestSchemes) {
        mLogger.d(TAG, "updateOrInsertRequestSchemes() called");

        List<String> processedRequestIds = new ArrayList<>(requestSchemes.size());

        for (RequestScheme requestScheme : requestSchemes) {
            RequestEntity request = convertSchemeToRequest(requestScheme);
            request = addReverseGeocodedLocation(request);
            mRequestsCacher.updateOrInsert(request);
            processedRequestIds.add(request.getId());
        }

        return processedRequestIds;
    }

    private RequestEntity addReverseGeocodedLocation(RequestEntity request) {
        String location = mReverseGeocoder.getFromLocation(
                request.getLatitude(),
                request.getLongitude(),
                Locale.getDefault());
        if (location != null) {
            return RequestEntity.getBuilder(request).setLocation(location).build();
        } else {
            return request;
        }
    }

    private RequestEntity convertSchemeToRequest(RequestScheme requestScheme) {
        return new RequestEntity(
                requestScheme.getId(),
                requestScheme.getCreatedBy(),
                requestScheme.getCreatedAt(),
                requestScheme.getCreatedComment(),
                parsePicturesList(requestScheme.getCreatedPictures()),
                requestScheme.getCreatedReputation(),
                requestScheme.getLatitude(),
                requestScheme.getLongitude(),
                requestScheme.getPickedUpBy(),
                requestScheme.getPickedUpAt(),
                requestScheme.getClosedBy(),
                requestScheme.getClosedAt(),
                requestScheme.getClosedComment(),
                parsePicturesList(requestScheme.getClosedPictures()),
                requestScheme.getClosedReputation(),
                "");
    }

    private List<String> parsePicturesList(String picturesListString) {
        if (picturesListString == null || picturesListString.isEmpty()) {
            return new ArrayList<>(0);
        } else {
            return Arrays.asList(picturesListString.split(Constants.PICTURES_LIST_SEPARATOR));
        }
    }

    private void notifyRequestsChanged() {
        mEventBus.post(new RequestsChangedEvent());
    }


}

package il.co.idocare.dependencyinjection.serversync;

import android.content.ContentResolver;

import org.greenrobot.eventbus.EventBus;

import dagger.Module;
import dagger.Provides;
import il.co.idocare.authentication.LoginStateManager;
import il.co.idocare.contentproviders.TransactionsController;
import il.co.idocare.location.ReverseGeocoder;
import il.co.idocare.networking.ServerApi;
import il.co.idocare.requests.cachers.RequestsCacher;
import il.co.idocare.requests.cachers.TempIdCacher;
import il.co.idocare.requests.retrievers.RawRequestRetriever;
import il.co.idocare.requests.retrievers.TempIdRetriever;
import il.co.idocare.serversync.syncers.RequestsSyncer;
import il.co.idocare.serversync.syncers.UserActionsSyncer;
import il.co.idocare.serversync.syncers.UsersSyncer;
import il.co.idocare.useractions.cachers.UserActionCacher;
import il.co.idocare.useractions.retrievers.UserActionsRetriever;
import il.co.idocare.users.UsersCacher;
import il.co.idocare.users.UsersRetriever;
import il.co.idocare.utils.Logger;
import il.co.idocare.utils.multithreading.BackgroundThreadPoster;

@Module
public class ServerSyncModule {

    @Provides
    @ServerSyncScope
    RequestsSyncer requestsSyncer(RequestsCacher requestsCacher,
                                  RawRequestRetriever rawRequestsRetriever,
                                  TransactionsController transactionsController,
                                  TempIdCacher tempIdCacher,
                                  ServerApi serverApi,
                                  ReverseGeocoder reverseGeocoder,
                                  EventBus eventBus,
                                  Logger logger) {
        return new RequestsSyncer(requestsCacher, rawRequestsRetriever, transactionsController,
                tempIdCacher, serverApi, reverseGeocoder, eventBus, logger);
    }

    @Provides
    @ServerSyncScope
    UserActionsSyncer userActionsSyncer(RequestsSyncer requestsSyncer,
                                        BackgroundThreadPoster backgroundThreadPoster,
                                        UserActionsRetriever userActionsRetriever,
                                        UserActionCacher userActionCacher,
                                        TempIdRetriever tempIdRetriever,
                                        ContentResolver contentResolver,
                                        ServerApi serverApi,
                                        Logger logger) {
        return new UserActionsSyncer(requestsSyncer, backgroundThreadPoster, userActionsRetriever,
                userActionCacher, tempIdRetriever, contentResolver, serverApi, logger);
    }
    @Provides
    @ServerSyncScope
    UsersSyncer usersSyncer(UsersRetriever usersRetriever,
                            UsersCacher usersCacher,
                            LoginStateManager loginStateManager,
                            ServerApi serverApi,
                            Logger logger) {
        return new UsersSyncer(usersRetriever, usersCacher, loginStateManager, serverApi, logger);
    }

}

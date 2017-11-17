package il.co.idocare.users;

import android.support.annotation.WorkerThread;

import org.greenrobot.eventbus.Subscribe;

import il.co.idocare.common.BaseManager;
import il.co.idocare.users.events.UserDataChangedEvent;
import il.co.idocare.utils.eventbusregistrator.EventBusRegistrable;
import il.co.idocare.utils.multithreading.BackgroundThreadPoster;
import il.co.idocare.utils.multithreading.MainThreadPoster;

/**
 * This manager encapsulates logic related to users
 */
@EventBusRegistrable
public class UsersDataMonitoringManager extends
        BaseManager<UsersDataMonitoringManager.UsersDataMonitorListener> {

    public interface UsersDataMonitorListener {
        /**
         * This method will be called for all listeners when data of a particular user changes
         * @param user up to date data of the user
         */
        void onUserDataChange(UserEntity user);
    }

    private UsersRetriever mUsersRetriever;
    private BackgroundThreadPoster mBackgroundThreadPoster;
    private MainThreadPoster mMainThreadPoster;

    public UsersDataMonitoringManager(UsersRetriever usersRetriever,
                                      BackgroundThreadPoster backgroundThreadPoster,
                                      MainThreadPoster mainThreadPoster) {
        mUsersRetriever = usersRetriever;
        mBackgroundThreadPoster = backgroundThreadPoster;
        mMainThreadPoster = mainThreadPoster;
    }

    /**
     * Asynchronously fetch data of a particular user and notify all the listeners if data
     * was found.
     */
    public void fetchUserByIdAndNotifyIfExists(final String userId) {
        mBackgroundThreadPoster.post(new Runnable() {
            @Override
            public void run() {
                fetchUserByIdAndNotifyIfExistsSync(userId);
            }
        });
    }

    @WorkerThread
    private void fetchUserByIdAndNotifyIfExistsSync(String userId) {
        UserEntity userEntity = mUsersRetriever.getUserById(userId);
        if (userEntity != null) {
            notifyUserDataFetched(userEntity);
        }
    }

    private void notifyUserDataFetched(final UserEntity userEntity) {
        mMainThreadPoster.post(new Runnable() {
            @Override
            public void run() {
                for (UsersDataMonitorListener listener : getListeners()) {
                    listener.onUserDataChange(userEntity);
                }
            }
        });
    }

    @Subscribe
    public void onUserDataChanged(UserDataChangedEvent event) {
        fetchUserByIdAndNotifyIfExists(event.getUserId());
    }


}

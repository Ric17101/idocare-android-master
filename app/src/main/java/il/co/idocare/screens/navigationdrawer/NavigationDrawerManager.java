package il.co.idocare.screens.navigationdrawer;

import android.support.annotation.UiThread;

import il.co.idocare.utils.Logger;

/**
 * This class encapsulates functionality related to NavigationDrawer
 */
@UiThread
public class NavigationDrawerManager {

    private NavigationDrawerDelegate mNavigationDrawerDelegate;
    private Logger mLogger;

    public NavigationDrawerManager(NavigationDrawerDelegate navigationDrawerDelegate,
                                   Logger logger) {
        mNavigationDrawerDelegate = navigationDrawerDelegate;
        mLogger = logger;
    }

    public void openDrawer() {
        mNavigationDrawerDelegate.openDrawer();
    }

    public void closeDrawer() {
        mNavigationDrawerDelegate.closeDrawer();
    }


}

package il.co.idocare.useractions.entities;

import android.support.annotation.NonNull;

import il.co.idocare.contentproviders.IDoCareContract;

/**
 * This entity encapsulates info about user's pick up action
 */

public class PickUpRequestUserActionEntity extends UserActionEntity {

    public static PickUpRequestUserActionEntity fromUserAction(UserActionEntity userAction) {
        return new PickUpRequestUserActionEntity(
                userAction.getDatetime(),
                userAction.getEntityId(),
                userAction.getActionParam());
    }

    public PickUpRequestUserActionEntity(String timestamp,
                                         @NonNull String requestId,
                                         @NonNull String pickedUpByUserId) {

        super(timestamp,
                IDoCareContract.UserActions.ENTITY_TYPE_REQUEST,
                requestId,
                null,
                IDoCareContract.UserActions.ACTION_TYPE_PICKUP_REQUEST,
                pickedUpByUserId);
    }

    public String getPickedUpByUserId() {
        return getActionParam();
    }

    public String getPickedUpAt() {
        return String.valueOf(getDatetime());
    }

}

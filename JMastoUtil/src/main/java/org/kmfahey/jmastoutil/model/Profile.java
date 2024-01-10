package org.kmfahey.jmastoutil.model;

import java.time.LocalDate;

public record Profile(
        String userId, int ftsRowId, int acctId, String userName, String instance, String URI,
        String fieldName1, String fieldValue1, String fieldName2, String fieldValue2, String
        fieldName3, String fieldValue3, String fieldName4, String fieldValue4, String profileText,
        LocalDate earliestNotification, boolean isLoginable, boolean hasBeenTested
) {
    public ProfileFtsRow toProfileFtsRow() {
        return new ProfileFtsRow(
                userId, userName, instance, URI, fieldName1, fieldValue1, fieldName2, fieldValue2, fieldName3,
                fieldValue3, fieldName4, fieldValue4, profileText
        );
    }
}
package com.marksandspencer.foodshub.pal.constant;

public enum  Status {

    COMPLETED("Completed"),
    IN_PROGRESS("In Progress"),
    YET_TO_START("Yet to Start"),
    DRAFT("Draft"),
    CREATIVE_STAGE("Creative Stage"),
    POST_CREATIVE_GATE("Post-Creative Gate"),
    FINALISE_STAGE("Finalise Stage"),
    POST_FINALISE_GATE("Post-Finalise Gate"),
    ARCHIVED("Archived"),
    DELETED("Deleted");

    private String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}

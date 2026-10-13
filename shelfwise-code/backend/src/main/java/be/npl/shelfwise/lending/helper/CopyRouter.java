package be.npl.shelfwise.lending.helper;

/** The routing table of section 8.6 as a pure function. */
public final class CopyRouter {

    public enum RoutingOutcome {
        /** The first waiting reservation is picked up at this branch: the copy is put on hold. */
        HOLD_HERE,
        /** The first waiting reservation is picked up elsewhere: the copy goes in transit to that branch. */
        TRANSIT_TO_PICKUP,
        AVAILABLE_HERE,
        TRANSIT_TO_HOME
    }

    private CopyRouter() {
    }

    /**
     * @param firstWaitingPickupBranchId pickup branch of the first WAITING reservation without an assigned copy,
     *                                   or null when there is none
     * @param returnToHomeBranch         false for copies that simply stay where they are when nothing is waiting
     *                                   (repair, hold expiry, cancelled hold)
     */
    public static RoutingOutcome decide(Long currentBranchId, Long homeBranchId, Long firstWaitingPickupBranchId,
                                        boolean returnToHomeBranch) {
        if (firstWaitingPickupBranchId != null) {
            return firstWaitingPickupBranchId.equals(currentBranchId)
                    ? RoutingOutcome.HOLD_HERE : RoutingOutcome.TRANSIT_TO_PICKUP;
        }
        if (!returnToHomeBranch || currentBranchId.equals(homeBranchId)) {
            return RoutingOutcome.AVAILABLE_HERE;
        }
        return RoutingOutcome.TRANSIT_TO_HOME;
    }
}

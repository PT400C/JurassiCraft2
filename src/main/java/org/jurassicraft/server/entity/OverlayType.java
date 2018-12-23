package org.jurassicraft.server.entity;


public enum OverlayType {
	
    EYE("eyes"), CLAW("claws"), EYELID("eyelid"), MOUTH("mouth"), NOSTRILS("nostrils"), STRIPES("stripes"), TEETH("teeth");

    public static final OverlayType[] VALUES = OverlayType.values();
    
    private final String identifier;

    OverlayType(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return this.identifier;
    }

}


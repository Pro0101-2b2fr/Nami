package namidevelopment.kiriyaga.api.contract.feature;

public interface RotationsFeatureConfig {

    RotationMode getRotationMode();
    JitterMode getJitterMode();

    double getRotationSpeed();
    double getRotationEase();
    double getRotationThreshold();

    boolean isMoveFixEnabled();
    boolean isRenderEnabled();
    int getHoldTicks();
    boolean isFutureRotations();

    enum RotationMode {
        MOTION, SILENT
    }

    enum JitterMode {
        NONE, GRIM, NORMAL
    }
}

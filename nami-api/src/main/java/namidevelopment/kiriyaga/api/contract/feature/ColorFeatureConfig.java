package namidevelopment.kiriyaga.api.contract.feature;

import java.awt.Color;

public interface ColorFeatureConfig {

    Color getGlobalColor();
    Color getFriendColor();
    Color getEnemyColor();

    boolean isRainbowEnabled();
    double getRainbowSpeed();

    Color getStyledGlobalColor();
    Color getStyledSecondColor();

    Color getStyledTextColor(int alpha);
}

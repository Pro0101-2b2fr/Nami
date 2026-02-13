package namidevelopment.kiriyaga.api.core.font;

public enum FontType {
    ARIAL("arial.ttf"),
    ARIALBD("arialbd.ttf"),
    ARIALBI("arialbi.ttf"),
    ARIALI("ariali.ttf"),
    ARIALBLK("arialblk.ttf"),
    VERDANA("verdana.ttf"),
    VERDANAB("verdanab.ttf"),
    VERDANAI("verdanai.ttf"),
    VERDANAZ("verdanaz.ttf");

    private final String fileName;

    FontType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}

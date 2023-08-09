package fr.jazer.session.utils.crypted;

public enum CertFormat {
    /**
     * Android.
     * If not supported try {@link CertFormat#JKS}.
     */
    BKS("BKS"),
    /**
     * Windows, Linux, MacOS, and others...
     * If not supported try {@link CertFormat#BKS}.
     */
    JKS("JKS");

    final String format;

    CertFormat(String format) {
        this.format = format;
    }
}

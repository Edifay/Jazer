package fr.jazer.session.utils.crypted;

public enum CertFormat {
    BKS("BKS"),
    JKS("JKS");

    final String format;

    CertFormat(String format) {
        this.format = format;
    }
}

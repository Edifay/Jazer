package fr.jazer.session.utils.crypted;

import java.io.InputStream;

public class ServerCertConfig {

    protected final InputStream input;
    protected final String password;
    protected final SecureType type;
    protected final CertFormat certFormat;

    public ServerCertConfig(final InputStream input, final String password, final SecureType type, final CertFormat certFormat) {
        this.input = input;
        this.password = password;
        this.type = type;
        this.certFormat = certFormat;
    }

    public InputStream input() {
        return this.input;
    }

    public String password() {
        return this.password;
    }


    public SecureType type() {
        return this.type;
    }

    public CertFormat format() {
        return this.certFormat;
    }
}

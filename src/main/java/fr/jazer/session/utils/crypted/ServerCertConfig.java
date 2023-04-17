package fr.jazer.session.utils.crypted;

import java.io.InputStream;

public record ServerCertConfig(InputStream input, String password, SecureType type, CertFormat certFormat) {
}

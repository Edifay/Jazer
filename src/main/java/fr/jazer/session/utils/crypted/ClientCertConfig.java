package fr.jazer.session.utils.crypted;

import java.io.InputStream;

public record ClientCertConfig(InputStream input, String password, SecureType secureType,
                               CertFormat format) {

}
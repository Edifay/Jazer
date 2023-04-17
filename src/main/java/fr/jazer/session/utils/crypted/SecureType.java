package fr.jazer.session.utils.crypted;
/*  Copyright (C) 2015 Gabriel POTTER (gpotter2)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
public enum SecureType {
    @Deprecated
    SSL("SSL"),
    @Deprecated
    SSLv2("SSLv2"),
    SSLv3("SSLv3"),
    @Deprecated
    TLS("TLS"),
    @Deprecated
    TLSv1("TLSv1"),
    TLSv1_1("TLSv1.1"),
    TLSv1_2("TLSv1.2");

    private final String type;

    private SecureType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
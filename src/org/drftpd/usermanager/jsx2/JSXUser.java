/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * DrFTPD is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * DrFTPD; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */
package org.drftpd.usermanager.jsx2;

import java.io.File;
import java.io.IOException;

import net.sf.drftpd.util.Crypt;
import net.sf.drftpd.util.SafeFileWriter;

import org.apache.log4j.Logger;
import org.drftpd.usermanager.AbstractUser;
import org.drftpd.usermanager.AbstractUserManager;
import org.drftpd.usermanager.PlainTextPasswordUser;
import org.drftpd.usermanager.UnixPassword;
import org.drftpd.usermanager.UserFileException;
import org.drftpd.usermanager.UserManager;

import JSX.ObjOut;


/**
 * @author mog
 * @version $Id: JSXUser.java,v 1.2 2004/11/06 07:55:35 mog Exp $
 */
public class JSXUser extends AbstractUser implements PlainTextPasswordUser,
    UnixPassword {
    private String _password;
    private String _unixPassword;
    private transient AbstractUserManager _usermanager;
    private boolean _purged;

    public JSXUser(JSXUserManager usermanager, String username) {
        super(username);
        _usermanager = usermanager;
    }

    public boolean checkPassword(String password) {
        if (this._password == null) {
            if (this._unixPassword == null) {
                throw new IllegalStateException("no password set");
            }

            if (this._unixPassword.equals(Crypt.crypt(
                            this._unixPassword.substring(0, 2), password))) {
                setPassword(password);

                return true;
            }

            return false;
        }

        return this._password.equals(password);
    }

    public void commit() throws UserFileException {
        if (_purged) {
            return;
        }

        try {
            if (!(_usermanager instanceof JSXUserManager)) {
                throw new ClassCastException(
                    "JSXUser without reference to JSXUserManager");
            }

            ObjOut out = new ObjOut(new SafeFileWriter(((JSXUserManager) _usermanager).getUserFile(
                            this.getUsername())));

            try {
                out.writeObject(this);
            } finally {
                out.close();
            }

            Logger.getLogger(JSXUser.class).debug("wrote " + getUsername());
        } catch (IOException ex) {
            throw new UserFileException("Error writing userfile for " +
                this.getUsername() + ": " + ex.getMessage(), ex);
        }
    }

    protected void finalize() throws Throwable {
        commit();
    }

    public String getPassword() {
        return this._password;
    }

    public String getUnixPassword() {
        return _unixPassword;
    }

    public void purge() {
        _purged = true;
        _usermanager.remove(this);

        if (!(_usermanager instanceof JSXUserManager)) {
            throw new ClassCastException(
                "JSXUser without reference to JSXUserManager");
        }

        File userfile = ((JSXUserManager) _usermanager).getUserFile(this.getUsername());
        userfile.delete();
    }

    public void setPassword(String password) {
        this._unixPassword = null;
        this._password = password;
    }

    public void setUnixPassword(String password) {
        this._password = null;
        this._unixPassword = password;
    }

    void setUserManager(AbstractUserManager um) {
        _usermanager = um;
    }

    public void update() {
        //an update was made, but commit() should be called from all places so
        // we don't need to do anything.
        //if we do, make sure it's implemented in all set and update methods in
        // AbstractUser
    }

    public UserManager getUserManager() {
        return _usermanager;
    }

	public AbstractUserManager getAbstractUserManager() {
		return _usermanager;
	}
}
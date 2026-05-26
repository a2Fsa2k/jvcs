package jvcs.exceptions;

import jvcs.hash.Hash;

public class ObjectNotFoundException extends JvcsException {
    public ObjectNotFoundException(Hash hash) {
        super("Object not found: " + hash.value());
    }
}

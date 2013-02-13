package net.ontrack.client.support;

import net.sf.jstring.support.CoreException;
import org.apache.commons.lang3.ObjectUtils;

public class ClientCannotLoginException extends CoreException {

	public ClientCannotLoginException(Object request) {
		super(ObjectUtils.toString(request, ""));
	}

}

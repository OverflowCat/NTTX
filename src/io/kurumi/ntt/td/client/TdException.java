package io.kurumi.ntt.td.client;

import io.kurumi.ntt.td.TdApi;

public class TdException extends Exception {
	
	private TdApi.Error error;

	public TdException(TdApi.Error error) {
		
		this.error = error;
		
	}

	public TdApi.Error getError() {
		return error;
	}
	
}
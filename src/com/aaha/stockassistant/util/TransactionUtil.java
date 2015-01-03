package com.aaha.stockassistant.util;

import com.aaha.stockassistant.util.Constants.AmountType;
import com.aaha.stockassistant.util.Constants.TransactionType;

public class TransactionUtil {

	public static AmountType getAmountType(TransactionType transactionType) {
		switch (transactionType) {
		case BUY:
		case BUY_BACK:
			return AmountType.TO_SHARES;
		case SELL:
		case SHORT:
			return AmountType.FROM_SHARES;
		}
		return null;
	}

}

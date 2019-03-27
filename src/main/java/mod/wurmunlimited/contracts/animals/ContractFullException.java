package mod.wurmunlimited.contracts.animals;

import com.wurmonline.shared.exceptions.WurmServerException;

public class ContractFullException extends WurmServerException {
    ContractFullException(String message) {
        super(message);
    }
}

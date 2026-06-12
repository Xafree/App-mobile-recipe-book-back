package fr.gymgod.etl.domain.port;

import fr.gymgod.etl.domain.model.AdditiveData;
import java.util.List;

public interface AdditiveSourcePort {
    List<AdditiveData> fetchAdditives();
}

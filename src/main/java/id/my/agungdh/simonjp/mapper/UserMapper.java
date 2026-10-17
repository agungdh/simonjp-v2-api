package id.my.agungdh.simonjp.mapper;

import id.my.agungdh.simonjp.domain.User;
import id.my.agungdh.simonjp.dto.MeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "pegawaiId", source = "pegawai.id")
    @Mapping(target = "nama", source = "pegawai.nama")
    @Mapping(target = "nip", source = "pegawai.nip")
    MeResponse toMeResponse(User user);
}

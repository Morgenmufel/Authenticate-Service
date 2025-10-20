package renatius.authenticationservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import renatius.authenticationservice.dto.UserDto;
import renatius.authenticationservice.entity.User;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    UserDto toDto(User user);

    User toEntity(UserDto userDto);
}

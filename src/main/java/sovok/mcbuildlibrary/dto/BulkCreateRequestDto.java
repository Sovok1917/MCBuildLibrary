package sovok.mcbuildlibrary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateRequestDto {

    @Valid
    @Size(max = 1000, message = "Author list cannot exceed 1000 entries")
    private List<NameDto> authors;

    @Valid
    @Size(max = 1000, message = "Theme list cannot exceed 1000 entries")
    private List<NameDto> themes;

    @Valid
    @Size(max = 1000, message = "Color list cannot exceed 1000 entries")
    private List<NameDto> colors;

    public record NameDto(
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String name
    ) {}
}
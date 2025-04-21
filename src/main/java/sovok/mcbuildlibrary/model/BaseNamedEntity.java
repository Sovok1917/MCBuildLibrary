package sovok.mcbuildlibrary.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import lombok.AccessLevel; // Import if needed for constructor below
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;


@MappedSuperclass
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Keep protected no-arg constructor for JPA
// on potential direct use/subclassing
@AllArgsConstructor // Keep AllArgsConstructor if needed for other parts or testing
@SuperBuilder
public abstract class BaseNamedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = StringConstants.NAME_NOT_BLANK)
    @Size(min = 2, message = StringConstants.NAME_SIZE)
    @Column(unique = true, nullable = false)
    @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseNamedEntity that)) {
            return false;
        }
        if (id != null && id > 0 && that.id != null && that.id > 0) {
            return Objects.equals(id, that.id);
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), name);
    }
}
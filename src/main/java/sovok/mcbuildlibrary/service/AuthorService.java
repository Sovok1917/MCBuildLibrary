package sovok.mcbuildlibrary.service;

import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.dao.AuthorDao;
import sovok.mcbuildlibrary.model.Author;

import java.util.Optional;

@Service
public class AuthorService {
    private final AuthorDao authorDao;

    public AuthorService(AuthorDao authorDao) {
        this.authorDao = authorDao;
    }

    public Author findOrCreateAuthor(String name) {
        Optional<Author> authorOpt = authorDao.findByName(name);
        if (authorOpt.isPresent()) {
            return authorOpt.get();
        } else {
            Author newAuthor = Author.builder().name(name).build();
            return authorDao.save(newAuthor);
        }
    }
}
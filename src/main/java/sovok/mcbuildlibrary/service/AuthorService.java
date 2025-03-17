package sovok.mcbuildlibrary.service;

import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.dao.AuthorDao;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;

import java.util.List;
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

    public Author createAuthor(String name) {
        if (authorDao.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Author with name '" + name + "' already exists");
        }
        Author author = Author.builder().name(name).build();
        return authorDao.save(author);
    }

    public Optional<Author> findAuthorById(Long id) {
        return authorDao.findById(id);
    }

    public List<Author> findAllAuthors() {
        List<Author> authors = authorDao.findAll();
        if (authors.isEmpty()) {
            throw new ResourceNotFoundException("No authors are currently available");
        }
        return authors;
    }

    public Author updateAuthor(Long id, String newName) {
        return authorDao.findById(id)
                .map(author -> {
                    if (authorDao.findByName(newName).isPresent() && !author.getName().equals(newName)) {
                        throw new IllegalArgumentException("Another author with name '" + newName + "' already exists");
                    }
                    author.setName(newName);
                    return authorDao.save(author);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Author with ID " + id + " not found"));
    }

    public void deleteAuthor(Long id) {
        if (!authorDao.existsById(id)) {
            throw new ResourceNotFoundException("Author with ID " + id + " not found");
        }
        authorDao.deleteById(id); // Cascading deletes builds and screenshots
    }
}
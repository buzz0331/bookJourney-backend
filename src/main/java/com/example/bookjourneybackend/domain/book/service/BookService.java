package com.example.bookjourneybackend.domain.book.service;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.GenreType;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.book.dto.request.GetBookListRequest;
import com.example.bookjourneybackend.domain.book.dto.response.BookInfo;
import com.example.bookjourneybackend.domain.book.dto.response.GetBookInfoResponse;
import com.example.bookjourneybackend.domain.book.dto.response.GetBookListResponse;
import com.example.bookjourneybackend.domain.book.dto.response.GetBookPopularResponse;
import com.example.bookjourneybackend.domain.favorite.domain.repository.FavoriteRepository;
import com.example.bookjourneybackend.global.exception.GlobalException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.example.bookjourneybackend.global.response.status.BaseExceptionResponseStatus.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final FavoriteRepository favoriteRepository;
    private final ObjectMapper objectMapper;
    private final BookCacheService bookCacheService;

    /**
     * 1. Redis에 있는지 확인하고 있으면 Redis에 value로 반환
     * 2. RestTemplate을 이용해 동기적으로 현재 페이지 불러와서 Redis에 저장 후 응답 전송
     * 3. RestTemplate을 이용해 비동기적으로 다음 페이지를 불러와서 Redis에 저장
     * Thread.start() 대신 CompletableFuture를 이용한 이유 : Thread Pool을 사용해 자원을 효율적으로 관리
     * @param getBookListRequest
     * @return
     */
    //todo Thread Pool Monitoring 로그 출력
    public GetBookListResponse searchBook(GetBookListRequest getBookListRequest) {
        log.info("------------------------[BookService.searchBook]------------------------");

        // 현재 페이지 데이터 가져오기
        String currentResponse = bookCacheService.getCurrentPage(getBookListRequest);

        // 비동기적으로 다음 페이지 캐싱
        CompletableFuture.runAsync(() -> {
            bookCacheService.getCurrentPage(getBookListRequest.IncreasePage());
            log.info("Next page caching completed for request page: {}", getBookListRequest.IncreasePage().getPage());
        });

        List<BookInfo> bookList = new ArrayList<>();

        //응답 JSON 데이터 파싱
        bookList = parseBookListFromResponse(currentResponse);


        log.info("Caching completed for current page.");
        log.info("currentResponse: {}", currentResponse);
        return GetBookListResponse.of(bookList);
    }

    private List<BookInfo> parseBookListFromResponse(String currentResponse) {
        List<BookInfo> bookList = new ArrayList<>();
        try{
            //JSON 형식 오류 허용 -> "Unrecognized character escape ''' (code 39)" 에러 해결용
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
//            currentResponse = currentResponse.replace("'", "\"");

            JsonNode root = objectMapper.readTree(currentResponse);
            JsonNode items = root.get("item");

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String title = item.get("title").asText();
                    String author = item.get("author").asText();

                    //isbn 13자리가 비어있는 경우 10자리 사용
                    String isbn = item.has("isbn13") && !item.get("isbn13").asText().isEmpty()
                            ? item.get("isbn13").asText()
                            : item.get("isbn").asText();
                    String cover = item.get("cover").asText();

//                    String link = item.get("link").asText();
                    String description = item.get("description").asText();
                    GenreType genreType = GenreType.parsingGenreType(item.get("categoryName").asText());
                    String publisher = item.get("publisher").asText();
                    String publishedDate = item.get("pubDate").asText();

                    GetBookInfoResponse g = bookCacheService.cachingBookInfo(title, author, isbn, cover, description, genreType.getGenreType(), publisher, publishedDate);

                    bookList.add(new BookInfo(g.getBookTitle(), g.getAuthorName(), g.getIsbn(), g.getImageUrl()));
                }
            }
        } catch (JsonProcessingException e) {
            log.info("Json 파싱 에러 메시지: {}", e.getMessage());
            throw new GlobalException(ALADIN_API_PARSING_ERROR);
        }
        return bookList;
    }

    public GetBookInfoResponse showBookInfo(String isbn, Long userId) {
        log.info("------------------------[BookService.showBookInfo]------------------------");
        GetBookInfoResponse getBookInfoResponse = bookCacheService.checkBookInfo(isbn);

        Optional<Book> findBook = bookRepository.findByIsbn(isbn);

        //레포지토리에 책이 존재하면..
        findBook.ifPresent(book -> {
            boolean isFavorite = favoriteRepository.existsActiveFavoriteByUserIdAndBook(userId, book);
            getBookInfoResponse.setFavorite(isFavorite);
        });

        bookCacheService.checkBookInfo(isbn);

        return getBookInfoResponse;
    }

    public GetBookPopularResponse showPopularBook() {
        return bookRepository.findBookWithMostRooms().stream()
                .findFirst()
                .map(book -> GetBookPopularResponse.of(
                        book.getBookId(),
                        book.getIsbn(),
                        book.getBookTitle(),
                        book.getImageUrl(),
                        book.getAuthorName(),
                        book.getRooms().size(),
                        book.getDescription()
                ))
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_POPULAR_BOOK));
    }

}

package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.Book;
import com.example.book.mapper.BookMapper;
import com.example.book.mapper.BorrowMapper;
import com.example.book.mapper.CategoryMapper;
import com.example.book.mapper.ReservationMapper;
import com.example.book.service.IBookService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图书资源管理服务实现
 * <p>所有写操作在执行前完成合法性校验（必填项、ISBN 唯一性、数字字段范围、分类有效性），
 * 校验不通过统一抛出 {@link BusinessException}，由全局异常处理器转换为友好提示，
 * 避免脏数据入库与接口报错。</p>
 */
@Service
public class BookServiceImpl implements IBookService {

    /** 搜索关键词最大长度，防止超长参数拖慢模糊查询 */
    private static final int MAX_KEYWORD_LENGTH = 100;
    /** 书名最大长度（与数据库字段一致） */
    private static final int MAX_TITLE_LENGTH = 200;
    /** 作者最大长度（与数据库字段一致） */
    private static final int MAX_AUTHOR_LENGTH = 100;
    /** ISBN 最大长度（与数据库字段一致） */
    private static final int MAX_ISBN_LENGTH = 20;
    /** 馆藏位置最大长度（与数据库字段一致） */
    private static final int MAX_LOCATION_LENGTH = 100;
    /** 简介最大长度 */
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    /** 馆藏库存上限，防止异常数据 */
    private static final int MAX_STOCK = 100000;
    /** 单次库存调整上限 */
    private static final int MAX_STOCK_DELTA = 1000;

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final BorrowMapper borrowMapper;
    private final ReservationMapper reservationMapper;

    public BookServiceImpl(BookMapper bookMapper, CategoryMapper categoryMapper,
                           BorrowMapper borrowMapper, ReservationMapper reservationMapper) {
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.borrowMapper = borrowMapper;
        this.reservationMapper = reservationMapper;
    }

    @Override
    public List<Book> list(String keyword, Integer categoryId, Boolean availableOnly, Integer userId) {
        // 检索参数容错：空串/纯空格视为不筛选；超长、特殊符号非法参数转为友好提示
        String safeKeyword = sanitizeKeyword(keyword);
        if (categoryId != null && categoryId <= 0) {
            throw new BusinessException(400, "分类参数不合法，请重新选择分类");
        }
        return bookMapper.findAll(safeKeyword, categoryId, availableOnly, userId);
    }

    @Override
    public Book getById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(404, "图书信息不存在");
        }
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new BusinessException(404, "图书信息不存在或已下架");
        }
        return book;
    }

    @Override
    public void add(Book book) {
        if (book == null) {
            throw new BusinessException(400, "图书信息不能为空");
        }
        validateBook(book);
        if (bookMapper.countByIsbn(book.getIsbn(), null) > 0) {
            throw new BusinessException(400, "该 ISBN 编号已存在，请勿重复入库");
        }
        if (bookMapper.insert(book) <= 0) {
            throw new BusinessException("添加图书失败，请稍后重试");
        }
    }

    @Override
    public void update(Book book) {
        if (book == null || book.getId() == null) {
            throw new BusinessException(400, "图书信息不能为空");
        }
        // 先校验目标图书是否存在，避免无效更新
        if (bookMapper.findById(book.getId()) == null) {
            throw new BusinessException(404, "图书不存在或已被下架，无法修改");
        }
        validateBook(book);
        if (bookMapper.countByIsbn(book.getIsbn(), book.getId()) > 0) {
            throw new BusinessException(400, "该 ISBN 编号已被其他图书占用");
        }
        if (bookMapper.update(book) <= 0) {
            throw new BusinessException("更新图书失败，请稍后重试");
        }
    }

    @Override
    public void delete(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(404, "图书信息不存在");
        }
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在或已被下架");
        }
        // 未完成的借阅业务优先校验，避免提前下架导致借阅业务逻辑混乱
        if (borrowMapper.countActiveByBookId(id) > 0) {
            throw new BusinessException(400, "该图书存在未完成的借阅记录，请先办理归还后再下架");
        }
        if (reservationMapper.countActiveByBookId(id) > 0) {
            throw new BusinessException(400, "该图书存在排队中的预约，请先处理预约后再下架");
        }
        if (bookMapper.deleteById(id) <= 0) {
            throw new BusinessException("下架图书失败，请稍后重试");
        }
    }

    @Override
    public int adjustStock(Integer id, Integer delta) {
        if (id == null || id <= 0) {
            throw new BusinessException(404, "图书信息不存在");
        }
        if (delta == null || delta == 0) {
            throw new BusinessException(400, "库存调整数量不能为空或为 0");
        }
        if (Math.abs(delta) > MAX_STOCK_DELTA) {
            throw new BusinessException(400, "单次库存调整数量不能超过 " + MAX_STOCK_DELTA + " 册");
        }
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在或已被下架，无法维护馆藏副本");
        }
        int current = book.getStock() == null ? 0 : book.getStock();
        int target = current + delta;
        if (target < 0) {
            throw new BusinessException(400, "馆藏库存数量不能小于 0（当前可借库存 " + current + " 册）");
        }
        if (target > MAX_STOCK) {
            throw new BusinessException(400, "馆藏库存数量不能超过 " + MAX_STOCK + " 册");
        }
        // SQL 带 stock + delta >= 0 条件，并发下也不会出现负库存
        if (bookMapper.adjustStock(id, delta) <= 0) {
            throw new BusinessException(400, "库存调整失败，请刷新页面后重试");
        }
        return target;
    }

    /**
     * 图书馆藏信息合法性校验：必填项、长度、数字字段范围
     */
    private void validateBook(Book book) {
        // 1. 必填项非空校验
        String title = trimToNull(book.getTitle());
        if (title == null) {
            throw new BusinessException(400, "书名不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(400, "书名长度不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
        String author = trimToNull(book.getAuthor());
        if (author == null) {
            throw new BusinessException(400, "作者不能为空");
        }
        if (author.length() > MAX_AUTHOR_LENGTH) {
            throw new BusinessException(400, "作者长度不能超过 " + MAX_AUTHOR_LENGTH + " 个字符");
        }
        String isbn = trimToNull(book.getIsbn());
        if (isbn == null) {
            throw new BusinessException(400, "ISBN 编号不能为空");
        }
        if (isbn.length() > MAX_ISBN_LENGTH) {
            throw new BusinessException(400, "ISBN 编号长度不能超过 " + MAX_ISBN_LENGTH + " 个字符");
        }
        if (!isValidIsbn(isbn)) {
            throw new BusinessException(400, "ISBN 编号格式不正确，应为 10 位或 13 位数字（可含连字符）");
        }
        String location = trimToNull(book.getLocation());
        if (location != null && location.length() > MAX_LOCATION_LENGTH) {
            throw new BusinessException(400, "馆藏位置长度不能超过 " + MAX_LOCATION_LENGTH + " 个字符");
        }
        String description = trimToNull(book.getDescription());
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(400, "简介长度不能超过 " + MAX_DESCRIPTION_LENGTH + " 个字符");
        }

        // 2. 分类合法性校验（分类编码必须真实存在）
        if (book.getCategoryId() == null) {
            throw new BusinessException(400, "请选择图书分类");
        }
        if (book.getCategoryId() <= 0 || categoryMapper.findById(book.getCategoryId()) == null) {
            throw new BusinessException(400, "图书分类不存在或已失效，请重新选择分类");
        }

        // 3. 数字字段格式与范围校验：禁止负数、非法数值
        if (book.getPrice() == null) {
            throw new BusinessException(400, "价格不能为空");
        }
        if (book.getPrice().signum() < 0) {
            throw new BusinessException(400, "价格不能为负数");
        }
        if (book.getPrice().stripTrailingZeros().scale() > 2) {
            throw new BusinessException(400, "价格最多保留两位小数");
        }
        if (book.getStock() == null) {
            throw new BusinessException(400, "馆藏库存数量不能为空");
        }
        if (book.getStock() < 0) {
            throw new BusinessException(400, "馆藏库存数量不能为负数");
        }
        if (book.getStock() > MAX_STOCK) {
            throw new BusinessException(400, "馆藏库存数量不能超过 " + MAX_STOCK + " 册");
        }

        // 4. 统一去除首尾空白，避免脏数据入库
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setLocation(location);
        book.setDescription(description);
    }

    /**
     * 检索关键词过滤：去除首尾空白与控制字符、Like 通配符，
     * 空关键词返回 null（等价于不加筛选），非法参数给出友好提示而不是抛出数据库异常
     */
    private String sanitizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(400, "搜索关键词长度不能超过 " + MAX_KEYWORD_LENGTH + " 个字符");
        }
        StringBuilder cleaned = new StringBuilder(trimmed.length());
        for (char c : trimmed.toCharArray()) {
            // % _ \ 为 Like 通配符/转义符，控制字符无检索意义，统一剔除
            if (c == '%' || c == '_' || c == '\\' || Character.isISOControl(c)) {
                continue;
            }
            cleaned.append(c);
        }
        String result = cleaned.toString().trim();
        if (result.isEmpty()) {
            throw new BusinessException(400, "搜索关键词包含特殊字符，请输入书名、作者或 ISBN 进行查询");
        }
        return result;
    }

    /** ISBN 合法性：去掉连字符与空格后应为 10 位（末位可为 X）或 13 位纯数字 */
    private boolean isValidIsbn(String isbn) {
        String normalized = isbn.replace("-", "").replace(" ", "");
        if (normalized.length() == 13) {
            return normalized.chars().allMatch(Character::isDigit);
        }
        if (normalized.length() == 10) {
            for (int i = 0; i < 10; i++) {
                char c = normalized.charAt(i);
                if (i == 9) {
                    if (!Character.isDigit(c) && c != 'X' && c != 'x') {
                        return false;
                    }
                } else if (!Character.isDigit(c)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

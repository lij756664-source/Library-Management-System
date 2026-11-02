package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.Category;
import com.example.book.mapper.CategoryMapper;
import com.example.book.service.ICategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements ICategoryService {

    /** 分类名称最大长度（与数据库字段一致） */
    private static final int MAX_NAME_LENGTH = 50;
    /** 分类描述最大长度（与数据库字段一致） */
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<Category> list() {
        return categoryMapper.findAll();
    }

    @Override
    public void add(Category category) {
        if (category == null) {
            throw new BusinessException(400, "分类信息不能为空");
        }
        validate(category);
        if (categoryMapper.findByName(category.getName()) != null) {
            throw new BusinessException(400, "分类名称已存在");
        }
        if (categoryMapper.insert(category) <= 0) {
            throw new BusinessException("添加分类失败，请稍后重试");
        }
    }

    @Override
    public void update(Category category) {
        if (category == null || category.getId() == null) {
            throw new BusinessException(400, "分类信息不能为空");
        }
        if (category.getId() <= 0) {
            throw new BusinessException(404, "分类不存在");
        }
        Category existing = categoryMapper.findById(category.getId());
        if (existing == null) {
            throw new BusinessException(404, "分类不存在或已被删除");
        }
        validate(category);
        Category sameName = categoryMapper.findByName(category.getName());
        if (sameName != null && !sameName.getId().equals(category.getId())) {
            throw new BusinessException(400, "分类名称已存在");
        }
        if (categoryMapper.update(category) <= 0) {
            throw new BusinessException("更新分类失败，请稍后重试");
        }
    }

    @Override
    public void delete(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(404, "分类不存在");
        }
        if (categoryMapper.findById(id) == null) {
            throw new BusinessException(404, "分类不存在或已被删除");
        }
        if (categoryMapper.countBooksByCategoryId(id) > 0) {
            throw new BusinessException(400, "该分类下仍有图书，无法删除");
        }
        if (categoryMapper.deleteById(id) <= 0) {
            throw new BusinessException("删除分类失败，请稍后重试");
        }
    }

    /**
     * 分类数据合法性校验：名称必填、长度限制，描述长度限制，统一去除首尾空白，拦截脏数据
     */
    private void validate(Category category) {
        String name = category.getName() == null ? null : category.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(400, "分类名称不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }
        String description = category.getDescription() == null ? null : category.getDescription().trim();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(400, "分类描述不能超过 " + MAX_DESCRIPTION_LENGTH + " 个字符");
        }
        category.setName(name);
        category.setDescription(description == null || description.isEmpty() ? null : description);
    }
}

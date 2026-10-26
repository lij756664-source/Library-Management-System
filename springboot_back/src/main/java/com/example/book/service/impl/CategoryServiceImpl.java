package com.example.book.service.impl;

import com.example.book.common.BusinessException;
import com.example.book.entity.Category;
import com.example.book.mapper.CategoryMapper;
import com.example.book.service.ICategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements ICategoryService {

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
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        if (category.getName().length() > 50) {
            throw new BusinessException(400, "分类名称不能超过50字符");
        }
        if (categoryMapper.findByName(category.getName()) != null) {
            throw new BusinessException(400, "分类名称已存在");
        }
        if (categoryMapper.insert(category) <= 0) {
            throw new BusinessException("添加分类失败");
        }
    }

    @Override
    public void update(Category category) {
        if (category.getId() == null) {
            throw new BusinessException(400, "分类ID不能为空");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        Category existing = categoryMapper.findById(category.getId());
        if (existing == null) {
            throw new BusinessException(404, "分类不存在");
        }
        Category sameName = categoryMapper.findByName(category.getName());
        if (sameName != null && !sameName.getId().equals(category.getId())) {
            throw new BusinessException(400, "分类名称已存在");
        }
        if (categoryMapper.update(category) <= 0) {
            throw new BusinessException("更新分类失败");
        }
    }

    @Override
    public void delete(Integer id) {
        if (categoryMapper.findById(id) == null) {
            throw new BusinessException(404, "分类不存在");
        }
        if (categoryMapper.countBooksByCategoryId(id) > 0) {
            throw new BusinessException(400, "该分类下仍有图书，无法删除");
        }
        if (categoryMapper.deleteById(id) <= 0) {
            throw new BusinessException("删除分类失败");
        }
    }
}

package cn.vincent.contoller;

import cn.vincent.tool.es.entity.index.ArticleIndex;
import cn.vincent.tool.es.entity.vo.ESPage;
import cn.vincent.tool.es.entity.vo.SearchValue;
import cn.vincent.tool.es.entity.vo.SearchVo;
import cn.vincent.tool.es.util.ElasticsearchUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/es")
public class HomeContoller {

    @Resource
    ElasticsearchUtil esUtil;

    //region 索引操作测试
    @GetMapping("/createIndex/{indexName}")
    public String createIndex(@PathVariable String indexName) {
        Map<String, String> prop = new HashMap<>();
        prop.put("title", "text");
        prop.put("body", "text");
        prop.put("postDate", "date");
        if (esUtil.createIndex(indexName, esUtil.mappingBuilder(prop)))
            return "创建成功";
        else
            return "创建失败";
    }

    @GetMapping("/createIndexByObj")
    public String createIndexByObj() {
        if (esUtil.createIndex(ArticleIndex.class))
            return "创建成功";
        else
            return "创建失败";
    }

    @GetMapping("/deleteIndex/{indexName}")
    public String deleteIndex(@PathVariable String indexName) {
        if (esUtil.deleteIndex(indexName))
            return "删除成功";
        else
            return "删除失败";
    }
    //endregion

    //region 文档操作测试
    @GetMapping("/saveDocument")
    public void saveDocument() {
        ArticleIndex article = ArticleIndex.builder()
                .title("标题")
                .body("内容")
                .postDate(new Date())
                .build();
        esUtil.insertDoc("article", article);
    }

    @GetMapping("/saveDocBatch")
    public void saveDocBatch() {
        esUtil.insertDocBatch("article", init());
    }
    //endregion

    //region 查询操作测试
    @GetMapping("/searchDoc/{keyword}")
    public String searchDoc(@PathVariable String keyword) {
        log.debug("请求处理");
        try {
            ObjectMapper mapper = new ObjectMapper();
            ESPage<ArticleIndex> article = esUtil.searchMatch("article", ArticleIndex.class, SearchVo.builder()
                    .field("body")
                    .value(SearchValue.builder().value(keyword).build())
                    .from(1)
                    .size(10)
                    .build());
            return mapper.writeValueAsString(article);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }
    //endregion

    private List<ArticleIndex> init() {
        List<ArticleIndex> list = new ArrayList<>();
        list.add(ArticleIndex.builder()
                .id("1")
                .title("现代风格")
                .body("现代风格的室内装饰以简洁、明亮、流畅的线条和几何形状为主要特征，强调功能性和实用性，注重空间的利用和布局。现代风格的颜色大多以白色、黑色、灰色为主，也会加入一些明亮的色彩来增加生气和活力。")
                .postDate(new Date())
                .build());

        list.add(ArticleIndex.builder()
                .id("2")
                .title("古典风格")
                .body("古典风格的室内装饰以华丽、精致、雕刻和浮雕为主要特征，注重对称和比例，强调质感和品质。古典风格的颜色大多以金色、银色、棕色为主，也会加入一些暗红色、暗绿色等深色调来增加气氛和厚重感。")
                .postDate(new Date())
                .build());

        list.add(ArticleIndex.builder()
                .id("3")
                .title("民族风格")
                .body("民族风格的室内装饰以传统、本土化、手工艺和图案为主要特征，注重文化和历史的传承，强调自然和原始的风格。民族风格的颜色大多以红色、黄色、蓝色为主，也会加入一些土黄色、棕色等暖色调来增加温暖和亲切感。")
                .postDate(new Date())
                .build());

        list.add(ArticleIndex.builder()
                .id("4")
                .title("工业风格")
                .body("工业风格的室内装饰以简洁、粗犷、原始和不加修饰的风格为主要特征，强调实用性和功能性，注重材料和结构的表现。工业风格的颜色大多以黑色、白色、铁锈色为主，也会加入一些深色调来增加震撼和沉稳感。")
                .postDate(new Date())
                .build());

        list.add(ArticleIndex.builder()
                .id("5")
                .title("自然风格")
                .body("自然风格的室内装饰以自然、舒适、简单和无拘无束的风格为主要特征，强调自然和生态的环境，注重自然材料和植物的运用。自然风格的颜色大多以绿色、棕色、米色为主，也会加入一些淡蓝色、淡黄色等淡色调来增加轻盈和清新感。")
                .postDate(new Date())
                .build());

        list.add(ArticleIndex.builder()
                .id("6")
                .title("艺术风格")
                .body("艺术风格的室内装饰以艺术、创意、个性和独特的风格为主要特征，强调表现和情感的表达，注重细节和手工艺的精湛。艺术风格的颜色多样，可以根据不同的主题和风格进行搭配，但一般不会过于鲜艳和刺眼。")
                .postDate(new Date())
                .build());

        list.add(ArticleIndex.builder()
                .id("7")
                .title("简约风格")
                .body("简约风格的室内装饰以简单、清爽、舒适和空间感为主要特征，强调功能性和实用性，注重线条和造型的简洁和流畅。简约风格的颜色大多以白色、黑色、灰色为主，也会加入一些淡色调和单一颜色来增加空间感和整洁感。")
                .postDate(new Date())
                .build());

        return list;
    }
}

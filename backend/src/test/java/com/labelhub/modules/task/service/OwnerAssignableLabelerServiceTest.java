package com.labelhub.modules.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.task.dto.AssignableLabelerResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class OwnerAssignableLabelerServiceTest {

    private final UserMapper userMapper = org.mockito.Mockito.mock(UserMapper.class);
    private final OwnerAssignableLabelerService service = new OwnerAssignableLabelerService(userMapper);

    @Test
    void listAssignableLabelersNormalizesKeywordAndPagination() {
        AssignableLabelerResponse labeler = new AssignableLabelerResponse(
                20L, "labeler-a", "labeler@example.com", "Labeler A", "/avatar.png", true, true);
        when(userMapper.countAssignableLabelers("labeler", true)).thenReturn(1L);
        when(userMapper.selectAssignableLabelers("labeler", true, 0, 100)).thenReturn(List.of(labeler));

        var page = service.listAssignableLabelers(" labeler ", true, 0, 500);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.pageSize()).isEqualTo(100);
        assertThat(page.total()).isEqualTo(1L);
        assertThat(page.items()).containsExactly(labeler);
        verify(userMapper).countAssignableLabelers("labeler", true);
        verify(userMapper).selectAssignableLabelers("labeler", true, 0, 100);
    }

    @Test
    void blankKeywordIsTreatedAsNoFilter() {
        when(userMapper.countAssignableLabelers(null, false)).thenReturn(0L);
        when(userMapper.selectAssignableLabelers(null, false, 20, 20)).thenReturn(List.of());

        var page = service.listAssignableLabelers("   ", false, 2, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(2);
        verify(userMapper).countAssignableLabelers(null, false);
        verify(userMapper).selectAssignableLabelers(null, false, 20, 20);
    }
}

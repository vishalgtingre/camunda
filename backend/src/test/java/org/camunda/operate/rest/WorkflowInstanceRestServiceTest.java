/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.operate.rest;

import org.apache.http.HttpStatus;
import org.camunda.operate.JacksonConfig;
import org.camunda.operate.es.reader.WorkflowInstanceReader;
import org.camunda.operate.property.OperateProperties;
import org.camunda.operate.util.MockMvcTestRule;
import org.camunda.operate.util.OperateIntegrationTest;
import org.camunda.operate.util.nobeans.TestApplicationWithNoBeans;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
  classes = {TestApplicationWithNoBeans.class, WorkflowInstanceRestService.class, JacksonConfig.class, OperateProperties.class}
)
public class WorkflowInstanceRestServiceTest extends OperateIntegrationTest {

  @Rule
  public MockMvcTestRule mockMvcTestRule = new MockMvcTestRule();

  @MockBean
  private WorkflowInstanceReader workflowInstanceReader;

  @Test
  public void testQueryWithWrongSortBy() throws Exception {
    //when
    String jsonRequest = "{ \"sorting\": {\"sortBy\": \"workflowId\",\"sortOrder\": \"asc\"}}";     //not allowed for sorting

    MockHttpServletRequestBuilder request = post(query(0, 100))
      .content(jsonRequest)
      .contentType(mockMvcTestRule.getContentType());

    //then
    final MvcResult mvcResult =
      mockMvcTestRule.getMockMvc().perform(request)
        .andExpect(status().is(HttpStatus.SC_BAD_REQUEST))
        .andReturn();

    assertThat(mvcResult.getResolvedException().getMessage()).contains("SortBy");

  }

  @Test
  public void testQueryWithWrongSortOrder() throws Exception {
    //when
    String jsonRequest = "{ \"sorting\": {\"sortBy\": \"id\",\"sortOrder\": \"unknown\"}}";     //wrong sort order

    MockHttpServletRequestBuilder request = post(query(0, 100))
      .content(jsonRequest)
      .contentType(mockMvcTestRule.getContentType());

    //then
    final MvcResult mvcResult =
      mockMvcTestRule.getMockMvc().perform(request)
        .andExpect(status().is(HttpStatus.SC_BAD_REQUEST))
        .andReturn();

    assertThat(mvcResult.getResolvedException().getMessage()).contains("SortOrder");

  }


  private String query(int firstResult, int maxResults) {
    return String.format("%s?firstResult=%d&maxResults=%d", WorkflowInstanceRestService.WORKFLOW_INSTANCE_URL, firstResult, maxResults);
  }

}

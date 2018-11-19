import React from 'react';
import ReactDOM from 'react-dom';
import {StyleGuide, DescribedMock, Embed} from './component-view';
import {Wizard} from './wizard';
import {SearchAFile} from './search-a-file';
import {VreLayout} from './clariah-vre';



ReactDOM.render(
  <span>
    <StyleGuide>
      <DescribedMock title="Clariah WP3">
        Components for Clariah WP3.

          <Embed caption="Wizard" description="Go through a proces in mutiple steps">
            <Wizard wizardSteps={[
                                {
                                  "stepLink": "1.html",
                                  "stepState": "done",
                                  "stepNumber": "1",
                                  "stepText": "Upload"
                                },
                                {
                                  "stepLink": "2.html",
                                  "stepState": "done",
                                  "stepNumber": "2",
                                  "stepText": "Configure"
                                }
                              ]} />
          </Embed>

          <Embed caption="Search a file" description="Search and select a file">
            <SearchAFile />
          </Embed>



        </DescribedMock>





    </StyleGuide>
    <VreLayout>
      <Wizard wizardSteps={[
                          {
                            "stepLink": "1.html",
                            "stepState": "done",
                            "stepNumber": "1",
                            "stepText": "Upload"
                          },
                          {
                            "stepLink": "2.html",
                            "stepState": "done",
                            "stepNumber": "2",
                            "stepText": "Configure"
                          }
                        ]} />
      <SearchAFile searchFileResults={[
                          {
                            "fileName": "File-text.txt",
                            "filePath": "c://docs/folder/otherfolder/folder-def/",
                            "fileType": "Plain text",
                            "fileUser": "John",
                            "fileDate": "21-12-2017"
                            },
                            {
                            "fileName": "Onthology.txt",
                            "filePath": "c://docs/folder/otherfolder/folder-def/",
                            "fileType": "Plain text",
                            "fileUser": "Paul",
                            "fileDate": "21-12-2017"
                            },
                            {
                            "fileName": "Results.txt",
                            "filePath": "c://docs/folder/otherfolder/folder-def/",
                            "fileType": "Plain text",
                            "fileUser": "George",
                            "fileDate": "21-12-2017"
                            },{
                            "fileName": "File-text.txt",
                            "filePath": "c://docs/folder/otherfolder/folder-def/",
                            "fileType": "Plain text",
                            "fileUser": "Ringo",
                            "fileDate": "21-12-2017"
                            },
                            {
                            "fileName": "Onthology.txt",
                            "filePath": "c://docs/folder/otherfolder/folder-def/",
                            "fileType": "Plain text",
                            "fileUser": "Paul",
                            "fileDate": "21-12-2017"
                            },
                            {
                            "fileName": "Results.txt",
                            "filePath": "c://docs/folder/otherfolder/folder-def/",
                            "fileType": "Plain text",
                            "fileUser": "George",
                            "fileDate": "21-12-2017"
                            }
                          ]}/>
    </VreLayout>
  </span>,
  document.getElementById('container')
);

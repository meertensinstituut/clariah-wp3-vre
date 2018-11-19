import React from 'react';
import './css/search-items.css';
//const Fragment = React.Fragment;


  export class SearchFileBox extends React.Component {
    render() {
      return (
        <div className="hc-search-items m5">
          <div className="hc-search-items-header m05">
            <div><strong>File search</strong></div>
            <div>
              <button className="hc-subtle">Save query</button>
              <button className="hc-subtle">Open query</button>
            </div>

          </div>
          <div className="hc-search-items-input m1-5"><input type="text" name="" value="" placeholder="type searchword or tag + <enter>" className="hc-input-big"/>
            <span>or</span>
            <button>Browse</button>
          </div>
          <div className="hc-search-items-selection" id="hc-search">
            <div className="smallGrey">Searched for: &nbsp;</div>

          </div>
        </div>

      );
    }
  }

  export class SearchResultsListItem extends React.Component {
    render() {
      return (
        <li>
          <div className="hc-list-item-primair hc-tooltip-show">
            <div className="itemSpread">
              <strong>{this.props.fileName}</strong>
              <div className="hc-tooltip"><a href="file-detail.html">View file</a></div>
            </div>

            <div className="smallGrey">{this.props.filePath}{this.props.fileName}</div>
          </div>
          <div className="hc-list-item-secun hc-tooltip-show">{this.props.fileType}</div>
          <div className="hc-list-item-tertair hc-tooltip-show">{this.props.fileUser}</div>
          <div className="hc-list-item-tertair hc-tooltip-show">{this.props.fileDate}</div>
          <div className="hc-list-item-tertair"><button>Connect</button></div>
        </li>

      );
    }
  }


  export class SearchResultsList extends React.Component {
    render() {
      return (

          <div id="searresults">
            <div className="hc-label-li">
              <div className="hc-label hc-list-item-primair">File</div>
              <div className="hc-label">Type</div>
              <div className="hc-label">User</div>
              <div className="hc-label">Date</div>
              <div className="hc-label"> </div>
            </div>
            <ul className="hc-list">
                {this.props.searchFileResults.map((searchFileResults) => <SearchResultsListItem
                  fileName={searchFileResults.fileName}
                  filePath={searchFileResults.filePath}
                  fileType={searchFileResults.fileType}
                  fileUser={searchFileResults.fileUser}
                  fileDate={searchFileResults.fileDate}
                 />)}
            </ul>
          </div>

      );
    }
  }





  export class SearchAFile extends React.Component {

    render() {
      return (
        <div>
          <SearchFileBox />
          <SearchResultsList />
        </div>
      );
    }
  }

import React from 'react';
import './css/style.css';
import './css/vre-top-bar.css';
import './images/logo-clariah-vre.svg';
const Fragment = React.Fragment;



  export class VreLayout extends React.Component {

    render() {
      return (
        <Fragment>
          <div className="hc-vreTopBar">
            <div className="siteWrap hc-vreTopBar-items">
              <div className="hc-vreTopBar-name">Clariah WP3 VRE</div>
              <div className="hc-vreTopBar-nav"></div>
              <div className="hc-vreTopBar-brand"></div>
            </div>
          </div>
          <div className="hc-site">
            <div className="siteWrap hc-form">
              {this.props.children}
            </div>
          </div>
        </Fragment>
      );
    }
  }

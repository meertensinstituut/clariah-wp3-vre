import React from 'react';
import './css/huc-data-entry.css';
import './css/huc-connect-sets.css';
const Fragment = React.Fragment;


  export class AtomImput extends React.Component {
    render() {
      return (
        <Fragment>
          <label htmlFor={this.props.name} className="form-label">{this.props.name}</label>
          <input type={this.props.text} name={this.props.name} placeholder={this.props.placeholder} />
        </Fragment>
      );
    }
  }
  export class AtomButton extends React.Component {
    render() {
      return (
        <Fragment>
          <button className="button-save button">
            {this.props.label}
          </button>
        </Fragment>
      );
    }
  }
  export class FromDescription extends React.Component {
    render() {
      return (
        <div className="huc-form-description">
          {this.props.description}
        </div>
      );
    }
  }


export class HcSearch extends React.Component {
  render() {
    return (
      <div className="huc-form-element">
        <label htmlFor={this.props.name} className="huc-form-label">{this.props.label}</label>
        <FromDescription description={this.props.description} />
        <div className="huc-connect-elements">
          <input type="text" name={this.props.name} placeholder={this.props.placeholder} />
          <button onClick={this.props.onClick} className="button icon iconSearch"></button>
        </div>
      </div>
    );
  }
}

export class HcFormInput extends React.Component {
  render() {
    return (
      <div className="huc-form-element">
        <label htmlFor={this.props.name} className="huc-form-label">{this.props.label}</label>
        <FromDescription description={this.props.description} />
        <input type="text" name={this.props.name} placeholder={this.props.placeholder} />
      </div>
    );
  }
}



export class HcFormImage extends React.Component {
  render() {
    return (
      <div className="huc-form-element">
        <label htmlFor={this.props.name} className="huc-form-label">{this.props.label}</label>
        <FromDescription description={this.props.description} />
        <input type="file" name={this.props.name}  />
      </div>
    );
  }
}

export class HcFormDate extends React.Component {
  render() {
    return (
      <div className="huc-form-element">
        <label htmlFor={this.props.name} className="huc-form-label">{this.props.label}</label>
        <FromDescription description={this.props.description} />
        <div className="huc-connect-elements">
          <input type="text" name={this.props.name} placeholder={this.props.placeholder} />
          <button className="button icon iconDate"></button>
        </div>
      </div>
    );
  }
}


export class HcFormPerson extends React.Component {
  render() {
    return (
      <div className="huc-form-element">
        <div className="huc-form-label">Person</div>
        <FromDescription description={this.props.description} />
        <div className="huc-fieldgroup">
          <div className="huc-fieldgroup-item">
            <label htmlFor={this.props.name1} className="huc-form-label">First name</label>
            <input type="text" name={this.props.name1} />
          </div>

          <div className="huc-fieldgroup-item">
            <label htmlFor={this.props.name2} className="huc-form-label">Last name</label>
            <input type="text" name={this.props.name2} />
          </div>
        </div>
      </div>
    );
  }
}

export class HcFormCoordinats extends React.Component {
  render() {
    return (
      <div className="huc-form-element">
        <div className="huc-form-label">Coordinates</div>
        <FromDescription description={this.props.description} />
        <div className="huc-fieldgroup">
          <div className="huc-fieldgroup-item">
            <label htmlFor={this.props.name1} className="huc-form-label">Longitude</label>
            <input type="text" name={this.props.name1} />
          </div>

          <div className="huc-fieldgroup-item">
            <label htmlFor={this.props.name2} className="huc-form-label">Latitude</label>
            <input type="text" name={this.props.name2} />
          </div>
        </div>
      </div>
    );
  }
}


export class HclistBox extends React.Component {
  render() {
    return (
      <div>
        <div className="huc-ConnectSets-header">
          <div className="huc-ConnectSets-title"><strong>{this.props.name}</strong></div>
          <div className="huc-ConnectSets-filter"> <input type="text" className="list-filter" placeholder="Filter"/></div>
        </div>
        <div className="list-container">
          <table className="huc-list">
            <tr>
              <td>{this.props.name} 1</td>
              <td>Clariah</td>
            </tr>
            <tr>
              <td>{this.props.name} 2</td>
              <td>Huc</td>
            </tr>
            <tr className="huc-list-active">
              <td>{this.props.name} 3</td>
              <td>Clariah</td>
            </tr>
            <tr>
              <td>{this.props.name} 4</td>
              <td>Huc</td>
            </tr>
            <tr>
              <td>{this.props.name} 5</td>
              <td>Clariah</td>
            </tr>
            <tr>
              <td>{this.props.name} 6</td>
              <td>Huc</td>
            </tr>
            <tr>
              <td>{this.props.name} 7</td>
              <td>Clariah</td>
            </tr>
            <tr>
              <td>{this.props.name} 8</td>
              <td>Huc</td>
            </tr>
            <tr>
              <td>{this.props.name} 9</td>
              <td>Clariah</td>
            </tr>
            <tr>
              <td>{this.props.name} 10</td>
              <td>Huc</td>
            </tr>
            <tr>
              <td>{this.props.name} 11</td>
              <td>Clariah</td>
            </tr>
            <tr>
              <td>{this.props.name} 12</td>
              <td>Huc</td>
            </tr>
          </table>
        </div>
      </div>
    );
  }
}


export class HcConnectSets extends React.Component {
  render() {
    return (
      <div class="huc-ConnectSets">
        <div class="rootList">
          <HclistBox name="Resources" />
        </div>
        <div class="col2">
          <div class="dependList">
            <HclistBox name="Services" />
          </div>

          <div class="huc-console">
            <div class="huc-console-message">Proces Resource 3 (Clariah) with:<br/>
            Service 2 (Wp2)?
            </div>
            <div class="huc-console-action"><button>Deploy</button></div>
          </div>
        </div>
      </div>
    );
  }
}


export class HcCombo extends React.Component {
    state = {
      teams: [{"value": "vtest1","display": "dtest1"},{"value": "vtest2","display": "dtest2"}]
    }

    render() {
      return (
        <div>
          <select>
            {this.state.teams.map((team) => <option key={team.value} value={team.value}>{team.display}</option>)}
          </select>
        </div>
      )
    }
  }


export class HcFormItemBrowser extends React.Component {
  render() {
    return (
      <div className="huc-form-nBrows">
        <div className="huc-form-nBrows-head">
          {this.props.label}
        </div>
        <div className="huc-form-nBrows-window">
          <div className="huc-list-view">
            <ul className="huc-form-nBrows-list huc-list">
              <li>John</li>
              <li>Paul</li>
              <li className="huc-list-selected">George</li>
              <li>Ringo</li>
            </ul>

          </div>
          <div className="huc-form-nBrows-edit">
          <HcFormPerson description="" />
            <div className="huc-action-bar">
              <div className="button"></div>
              <div className="button">New Person</div>
              <div className="button"></div>
            </div>

          </div>
        </div>
      </div>
    );
  }
}

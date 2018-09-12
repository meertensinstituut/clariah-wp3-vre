import React from "react";
import Switchboard from "../common/switchboard";
import Spinner from "../common/spinner";
import {Alert, Panel} from "react-bootstrap";
import PropTypes from 'prop-types';

import './poll.css';
import {DeploymentStatus} from "../common/deployment-status";

export default class Poll extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            workDir: this.props.match.params.workDir,
            deployStatus: null,
            httpStatus: null,
            opened: false,
            polling: false
        };
    }

    componentDidMount() {
        this.pollDeployment();
    }

    pollDeployment() {
        this.setState({polling: true});
        Switchboard.getDeploymentStatusResult(this.state.workDir).done((data) => {
            // TODO: use actual http status code:
            const httpStatus = 200;
            const deployStatus = data;
            this.setState({httpStatus, deployStatus, polling: false}, () => {
                const toPoll = ['DEPLOYED', 'RUNNING'];
                if (toPoll.includes(deployStatus.status)) {
                    const timeout = this.props.interval;
                    setTimeout(() => this.pollDeployment(), timeout);
                }
            });
        }).fail((xhr) => {
            const httpStatus = xhr.status;
            const deployStatus = {status: xhr.responseJSON.msg};
            this.setState({httpStatus, deployStatus, polling: false});
        });
    }

    handlePanelClick = () => {
        this.setState({opened: !this.state.opened});
    };

    render() {
        const deployStatus = this.state.deployStatus;

        let jsonStatus = this.state.httpStatus
            ? <div>
                <h3>HTTP Status code:</h3>
                <pre>{this.state.httpStatus}</pre>
                <h3>Response:</h3>
                <pre>{JSON.stringify(deployStatus, null, 2)}</pre>
            </div>
            : null;

        let alert = ![200, null].includes(this.state.httpStatus)
            ? <Alert bsStyle="danger">{this.state.deployStatus.status}</Alert>
            : null;


        let statusTable = this.state.httpStatus === 200
            ? <table className="deployment-status">
                <tbody>
                <tr>
                    <td>status</td>
                    <td>{deployStatus.status}</td>
                </tr>
                <tr>
                    <td>service</td>
                    <td>{deployStatus.service}</td>
                </tr>
                <tr>
                    <td>work directory</td>
                    <td>{deployStatus.workDir}</td>
                </tr>
                <tr>
                    <td>input files</td>
                    <td>{deployStatus.files.map(
                        (f, i) => <div key={i}>{f}</div>
                    )}</td>
                </tr>
                {deployStatus.status === DeploymentStatus.FINISHED
                    ? <tr>
                        <td>output folder</td>
                        <td>{deployStatus.outputDir}</td>
                    </tr>
                    : null
                }
                </tbody>
            </table>
            : null;

        return (
            <div>
                <Panel>
                    <Panel.Heading>
                        <Panel.Title>
                            Deployment status of <code>{this.state.workDir}</code>
                            <span className="pull-right">
                            {deployStatus && deployStatus.status === 'RUNNING' ? <Spinner/> : <i className="fa fa-check-square-o" aria-hidden="true"/>}
                            </span>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Body>
                        {alert}
                        {statusTable}
                    </Panel.Body>
                </Panel>
                <Panel>
                    <Panel.Heading>
                        <Panel.Title
                            className='clickable'
                            onClick={this.handlePanelClick}
                        >
                            Details
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Body
                        className={this.state.opened ? '' : 'collapse'}
                    >
                        {jsonStatus}
                    </Panel.Body>
                </Panel>
            </div>
        );
    }
}

Poll.propTypes = {
    match: PropTypes.shape({
        params: PropTypes.shape({
            workDir: PropTypes.string.isRequired
        })
    })
};

Poll.defaultProps = {
    interval: 1000
};
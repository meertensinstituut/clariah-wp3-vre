import React from "react";

import './error-msg.css';

/**
 * Expects error body with msg field containing error
 */
export default class ErrorMsg extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.state = {
            show: true,
            error: this.props.error
        };
    }

    /**
     * If props change, show new error msg again
     */
    static getDerivedStateFromProps(nextProps, prevState) {
        if(nextProps.error === prevState.error) {
            return {};
        }
        return {error: nextProps.error, show: true};
    }

    handleDismiss = () => {
        this.setState({show: false});
    };

    render() {
        if (!this.state.show) {
            return null;
        }
        if (!this.props.error) {
            return null;
        }

        // TODO:
        let msg = "";
        if(this.state.error.msg) {
            msg = this.state.error.msg;
        }
        if(this.state.error.message) {
            msg = this.state.error.message;
        }

        console.error(this.props.error);
        return (
            <div role="alert" className="alert alert-danger alert-dismissable">
                <button type="button" className="pull-right" onClick={() => this.handleDismiss()}>
                    <span aria-hidden="true">×</span>
                </button>
                <p className="vre-error-msg">{this.props.title}: {msg}</p>
            </div>
        );
    }
}

ErrorMsg.defaultProps = {
    error: null,
    title: "Error"
};
